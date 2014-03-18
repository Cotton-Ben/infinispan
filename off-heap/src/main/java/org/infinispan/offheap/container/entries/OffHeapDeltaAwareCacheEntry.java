package org.infinispan.offheap.container.entries;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.CopyableDeltaAware;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commons.util.Util;
import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

import static org.infinispan.offheap.container.entries.OffHeapDeltaAwareCacheEntry.Flags.EVICTED;
import static org.infinispan.offheap.container.entries.OffHeapDeltaAwareCacheEntry.Flags.REMOVED;
import static org.infinispan.offheap.container.entries.OffHeapDeltaAwareCacheEntry.Flags.CHANGED;
import static org.infinispan.offheap.container.entries.OffHeapDeltaAwareCacheEntry.Flags.VALID;
import static org.infinispan.offheap.container.entries.OffHeapDeltaAwareCacheEntry.Flags.CREATED;

/**
 * A wrapper around a cached entry that encapsulates DeltaAware and Delta semantics when writes are
 * initiated, committed or rolled back.
 * 
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @since 5.1
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public class OffHeapDeltaAwareCacheEntry implements OffHeapCacheEntry, OffHeapStateChangingEntry {
   private static final Log log = LogFactory.getLog(OffHeapDeltaAwareCacheEntry.class);
   private static final boolean trace = log.isTraceEnabled();

   protected Object key;
   protected OffHeapCacheEntry wrappedEntry;
   protected DeltaAware value, oldValue;
   protected final List<Delta> deltas;
   protected byte flags = 0;

   // add Map representing uncommitted changes
   protected AtomicHashMap<?, ?> uncommittedChanges;

   public OffHeapDeltaAwareCacheEntry(Object key, DeltaAware value, OffHeapCacheEntry wrappedEntry) {
      setValid(true);
      this.key = key;
      this.value = value;
      this.wrappedEntry = wrappedEntry;
      if (value instanceof AtomicHashMap) {
         this.uncommittedChanges = ((AtomicHashMap) value).copy();
      }
      this.deltas = new LinkedList<Delta>();
   }

   @Override
   public byte getStateFlags() {
      if (wrappedEntry instanceof OffHeapStateChangingEntry) {
         return ((OffHeapStateChangingEntry)wrappedEntry).getStateFlags();
      }

      return flags;
   }

   @Override
   public void copyStateFlagsFrom(OffHeapStateChangingEntry other) {
      this.flags = other.getStateFlags();
   }

   public void appendDelta(Delta d) {
      deltas.add(d);
      d.merge(uncommittedChanges);
      setChanged(true);
   }

   public AtomicHashMap<?, ?> getUncommittedChages() {
      return uncommittedChanges;
   }

   protected static enum Flags {
      CHANGED(1), // same as 1 << 0
      CREATED(1 << 1),
      REMOVED(1 << 2),
      VALID(1 << 3),
      EVICTED(1 << 4),
      LOADED(1 << 5),
      SKIP_LOOKUP(1 << 6);

      final byte mask;

      Flags(int mask) {
         this.mask = (byte) mask;
      }
   }

   /**
    * Tests whether a flag is set.
    *
    * @param flag
    *           flag to test
    * @return true if set, false otherwise.
    */
   protected final boolean isFlagSet(Flags flag) {
      return (flags & flag.mask) != 0;
   }

   /**
    * Utility method that sets the value of the given flag to true.
    *
    * @param flag
    *           flag to set
    */
   protected final void setFlag(Flags flag) {
      flags |= flag.mask;
   }

   /**
    * Utility method that sets the value of the flag to false.
    *
    * @param flag
    *           flag to unset
    */
   protected final void unsetFlag(Flags flag) {
      flags &= ~flag.mask;
   }

   @Override
   public final long getLifespan() {
      return -1;  // forever
   }

   @Override
   public final long getMaxIdle() {
      return -1;  // forever
   }

   @Override
   public boolean skipLookup() {
      return isFlagSet(OffHeapDeltaAwareCacheEntry.Flags.SKIP_LOOKUP);
   }

   @Override
   public final Object getKey() {
      return key;
   }

   @Override
   public final Object getValue() {
      return value;
   }

   @Override
   public final Object setValue(Object value) {
      Object oldValue = this.value;
      this.value = (DeltaAware) value;
      return oldValue;
   }

   //@Override
   //public void commit(OffHeapDataContainer container, Metadata metadata) {}

   @Override
   public boolean isNull() {
      return false;
   }

   @Override
   public final void commit(OffHeapDataContainer container, OffHeapMetadata metadata) {
      //If possible, we now ensure copy-on-write semantics. This way, it can ensure the correct transaction isolation.
      //note: this method is invoked under the ClusteringDependentLogic.lock(key)
      //note2: we want to merge/copy to/from the data container value.
      OffHeapCacheEntry entry = container.get(key);
      DeltaAware containerValue = entry == null ? null : (DeltaAware) entry.getValue();
      if (containerValue != null && containerValue != value) {
         value = containerValue;
      }
      if (value != null && !deltas.isEmpty()) {
         final boolean makeCopy = value instanceof CopyableDeltaAware;
         if (makeCopy) {
            value = ((CopyableDeltaAware) value).copy();
         }
         for (Delta delta : deltas) {
            delta.merge(value);
         }
         if (makeCopy) {
            container.put(key, value, extractMetadata(entry, metadata));
         }
         value.commit();
         if (wrappedEntry != null) {
            wrappedEntry.setChanged(!makeCopy);
         }
      }
      reset();
      // only do stuff if there are changes.
      if (wrappedEntry != null) {
         wrappedEntry.commit(container, metadata);
      }
   }

   private OffHeapMetadata extractMetadata(OffHeapCacheEntry entry, OffHeapMetadata provided) {
      if (provided != null) {
         return provided;
      } else if (wrappedEntry != null) {
         return wrappedEntry.getMetadata();
      }
      return entry == null ? null : entry.getMetadata();
   }

   private void reset() {
      oldValue = null;
      deltas.clear();
      flags = 0;
      if (uncommittedChanges != null) {
         uncommittedChanges.clear();
      }
      setValid(true);
   }

   @Override
   public final void rollback() {
      if (isChanged()) {
         value = oldValue;
         reset();
      }
   }

   @Override
   public final boolean isChanged() {
      return isFlagSet(CHANGED);
   }

   @Override
   public final void setChanged(boolean changed) {
      setFlag(changed, CHANGED);
   }

   @Override
   public boolean isValid() {
      if (wrappedEntry != null) {
         return wrappedEntry.isValid();
      } else {
         return isFlagSet(VALID);
      }
   }

   @Override
   public final void setValid(boolean valid) {
      setFlag(valid, VALID);
   }

   @Override
   public final boolean isCreated() {
      if (wrappedEntry != null) {
         return wrappedEntry.isCreated();
      } else {

         return isFlagSet(CREATED);
      }
   }

   @Override
   public final void setCreated(boolean created) {
      setFlag(created, CREATED);
   }

   @Override
   public boolean isRemoved() {
      if (wrappedEntry != null) {
         return wrappedEntry.isRemoved();
      } else {
         return isFlagSet(REMOVED);
      }
   }

   @Override
   public boolean isEvicted() {
      if (wrappedEntry != null) {
         return wrappedEntry.isEvicted();
      } else {
         return isFlagSet(EVICTED);
      }
   }

   @Override
   public final void setRemoved(boolean removed) {
      setFlag(removed, REMOVED);
   }

   @Override
   public void setEvicted(boolean evicted) {
      setFlag(evicted, EVICTED);
   }

   @Override
   public boolean isLoaded() {
      return isFlagSet(OffHeapDeltaAwareCacheEntry.Flags.LOADED);
   }

   @Override
   public void setLoaded(boolean loaded) {
      setFlag(loaded, OffHeapDeltaAwareCacheEntry.Flags.LOADED);
   }

   @Override
   public void setSkipLookup(boolean skipLookup) {
      setFlag(skipLookup, OffHeapDeltaAwareCacheEntry.Flags.SKIP_LOOKUP);
   }

   private void setFlag(boolean enable, Flags flag) {
      if (enable)
         setFlag(flag);
      else
         unsetFlag(flag);
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "(" + Util.hexIdHashCode(this) + "){" + "key=" + key
               + ", value=" + value + ", oldValue=" + uncommittedChanges + ", isCreated="
               + isCreated() + ", isChanged=" + isChanged() + ", isRemoved=" + isRemoved()
               + ", isValid=" + isValid() + '}';
   }

   @Override
   public boolean undelete(boolean doUndelete) {
      if (isRemoved() && doUndelete) {
         if (trace)
            log.trace("Entry is deleted in current scope.  Un-deleting.");
         setRemoved(false);
         setValid(true);
         return true;
      }
      return false;
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return null;  // DeltaAware are always metadata unaware
   }

    @Override
    public void setMetadata(OffHeapMetadata metadata) {

    }


}
