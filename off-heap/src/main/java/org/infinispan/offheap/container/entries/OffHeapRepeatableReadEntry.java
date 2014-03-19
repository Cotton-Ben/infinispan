package org.infinispan.offheap.container.entries;

import org.infinispan.container.DataContainer;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.container.versioning.OffHeapEntryVersion;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.transaction.WriteSkewException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import static org.infinispan.offheap.container.entries.OffHeapReadCommittedEntry.Flags.COPIED;
import static org.infinispan.offheap.container.entries.OffHeapReadCommittedEntry.Flags.SKIP_LOOKUP;

/**
 * An extension of {@link OffHeapReadCommittedEntry} that provides Repeatable Read semantics
 *
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @since 4.0
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public abstract class OffHeapRepeatableReadEntry extends OffHeapReadCommittedEntry {
   private static final Log log = LogFactory.getLog(OffHeapRepeatableReadEntry.class);

   public OffHeapRepeatableReadEntry(Object key, Object value, OffHeapMetadata metadata) {
      super(key, value, metadata);
   }

//   @Override
//   public void copyForUpdate(OffHeapDataContainer container) {
//      if (isFlagSet(COPIED)) return; // already copied
//
//      setFlag(COPIED); //mark as copied
//
//      // make a backup copy
//      oldValue = value;
//   }

   public void performLocalWriteSkewCheck(OffHeapDataContainer container, boolean alreadyCopied) {
      // check for write skew.
      OffHeapInternalCacheEntry ice = container.get(key);

      Object actualValue = ice == null ? null : ice.getValue();
      Object valueToCompare = alreadyCopied ? oldValue : value;
      if (log.isTraceEnabled()) {
         log.tracef("Performing local write skew check. actualValue=%s, transactionValue=%s", actualValue, valueToCompare);
      }
      // Note that this identity-check is intentional.  We don't *want* to call actualValue.equals() since that defeats the purpose.
      // the implicit "versioning" we have in R_R creates a new wrapper "value" instance for every update.
      if (actualValue != null && actualValue != valueToCompare) {
         log.unableToCopyEntryForUpdate(getKey());
         throw new WriteSkewException("Detected write skew.", key);
      }

      if (valueToCompare != null && ice == null && !isCreated()) {
         // We still have a write-skew here.  When this wrapper was created there was an entry in the data container
         // (hence isCreated() == false) but 'ice' is now null.
         log.unableToCopyEntryForUpdate(getKey());
         throw new WriteSkewException("Detected write skew - concurrent removal of entry!", key);
      }
   }

    @Override
    public void copyForUpdate(OffHeapDataContainer container) {

    }

    public abstract OffHeapEntryVersion getVersion();


    public abstract void copyForUpdate(DataContainer container);

    @Override
   public boolean isNull() {
      return value == null;
   }

   @Override
   public void setSkipLookup(boolean skipLookup) {
      setFlag(skipLookup, SKIP_LOOKUP);
   }

   @Override
   public boolean skipLookup() {
      return isFlagSet(SKIP_LOOKUP);
   }
}
