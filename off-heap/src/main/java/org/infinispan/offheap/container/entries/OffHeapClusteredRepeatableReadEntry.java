package org.infinispan.offheap.container.entries;

import org.infinispan.container.DataContainer;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.container.entries.versioned.OffHeapVersioned;
import org.infinispan.offheap.container.versioning.OffHeapEntryVersion;
import org.infinispan.offheap.container.versioning.OffHeapInequalVersionComparisonResult;
import org.infinispan.offheap.container.versioning.OffHeapInequalVersionComparisonResult;
import org.infinispan.offheap.container.versioning.OffHeapVersionGenerator;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A version of RepeatableReadEntry that can perform write-skew checks during prepare.
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public class OffHeapClusteredRepeatableReadEntry extends OffHeapRepeatableReadEntry implements OffHeapVersioned {

   private static final Log log = LogFactory.getLog(OffHeapClusteredRepeatableReadEntry.class);

   public OffHeapClusteredRepeatableReadEntry(Object key, Object value, OffHeapMetadata metadata) {
      super(key, value, metadata);
   }

   public boolean performWriteSkewCheck(
                                OffHeapDataContainer container,
                                TxInvocationContext ctx,
                                OffHeapEntryVersion versionSeen,
                                OffHeapVersionGenerator versionGenerator) {
      if (versionSeen == null) {
         if (log.isTraceEnabled()) {
            log.tracef("Perform write skew check for key %s but the key was not read. Skipping check!", key);
         }
         //version seen is null when the entry was not read. In this case, the write skew is not needed.
         return true;
      }
      OffHeapEntryVersion prevVersion;
      OffHeapInternalCacheEntry ice = (OffHeapInternalCacheEntry) container.get(key);
      if (ice == null) {
         if (log.isTraceEnabled()) {
            log.tracef("No entry for key %s found in data container" , key);
         }
         prevVersion = (OffHeapEntryVersion) ctx.getCacheTransaction().getLookedUpRemoteVersion(key);
         if (prevVersion == null) {
            if (log.isTraceEnabled()) {
               log.tracef("No looked up remote version for key %s found in context" , key);
            }
            //in this case, the key does not exist. So, the only result possible is the version seen be the NonExistingVersion
             if (versionGenerator.nonExistingVersion().compareTo(versionSeen) == OffHeapInequalVersionComparisonResult.EQUAL)
                 return true;
             else return false;
         }
      } else {
         prevVersion = (OffHeapEntryVersion) ice.getMetadata().version();
         if (prevVersion == null)
            throw new IllegalStateException("Entries cannot have null versions!");
      }
      if (log.isTraceEnabled()) {
         log.tracef("Is going to compare versions %s and %s for key %s.", prevVersion, versionSeen, key);
      }

      //in this case, the transaction read some value and the data container has a value stored.
      //version seen and previous version are not null. Simple version comparation.
      OffHeapInequalVersionComparisonResult result = prevVersion.compareTo(versionSeen);
      if (log.isTraceEnabled()) {
         log.tracef("Comparing versions %s and %s for key %s: %s", prevVersion, versionSeen, key, result);
      }
      return OffHeapInequalVersionComparisonResult.AFTER != result;
   }

   // This entry is only used when versioning is enabled, and in these
   // situations, versions are generated internally and assigned at a
   // different stage to the rest of metadata. So, keep the versioned API
   // to make it easy to apply version information when needed.

   @Override
   public OffHeapEntryVersion getVersion() {
      return (OffHeapEntryVersion) metadata.version();
   }


    @Override
   public void setVersion(OffHeapEntryVersion version) {
      metadata = metadata.builder().version((OffHeapEntryVersion) version).build();
   }

    @Override
    public void copyForUpdate(DataContainer container) {

    }

    @Override
   public boolean isNull() {
      return value == null;
   }


}
