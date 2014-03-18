package org.infinispan.offheap.util;

import org.infinispan.commons.util.Immutables;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheValue;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.util.CoreImmutables;

import static org.infinispan.commons.util.Util.toStr;


/**
 * Factory for generating immutable type wrappers for core types.
 */
public class OffHeapCoreImmutables extends Immutables {

   /**
    * Wraps a {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry}}
    * with an immutable {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry}}.
    * There is no copying involved.
    *
    *
    * @param entry the internal cache entry to wrap.
    * @return an immutable {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry}}
    * wrapper that delegates to the original entry.
    */
   public static OffHeapInternalCacheEntry immutableInternalCacheEntry(OffHeapInternalCacheEntry entry) {
      return (OffHeapInternalCacheEntry) new OffHeapImmutableInternalCacheEntry((OffHeapInternalCacheEntry) entry);
   }

   /*
    public static InternalCacheEntry immutableInternalCacheEntry(OffHeapInternalCacheEntry entry) {

        return new ImmutableInternalCacheEntry(entry);
    }
    */
   /**
    * Immutable version of InternalCacheEntry for traversing data containers.
    */
   private static class OffHeapImmutableInternalCacheEntry implements OffHeapInternalCacheEntry, Immutable {
      private final OffHeapInternalCacheEntry entry;
      private final int hash;

      OffHeapImmutableInternalCacheEntry(OffHeapInternalCacheEntry entry) {
         this.entry = entry;
         this.hash = entry.hashCode();
      }

      @Override
      public Object getKey() {
         return entry.getKey();
      }

      @Override
      public Object getValue() {
         return entry.getValue();
      }

      @Override
      public Object setValue(Object value) {
         throw new UnsupportedOperationException();
      }

       @Override
       public void commit(OffHeapDataContainer container, OffHeapMetadata metadata) {

       }

//      @Override
//      public void commit(OffHeapDataContainer container, OffHeapMetadata metadata) {
//         throw new UnsupportedOperationException();
//      }

      @Override
      @SuppressWarnings("unchecked")
      public boolean equals(Object o) {
         if (!(o instanceof InternalCacheEntry))
            return false;

         InternalCacheEntry entry = (InternalCacheEntry) o;
         return entry.equals(this.entry);
      }

      @Override
      public int hashCode() {
         return hash;
      }

      @Override
      public String toString() {
         return toStr(getKey()) + "=" + toStr(getValue());
      }

      @Override
      public boolean canExpire() {
         return entry.canExpire();
      }

      @Override
      public long getCreated() {
         return entry.getCreated();
      }

      @Override
      public long getExpiryTime() {
         return entry.getExpiryTime();
      }

      @Override
      public long getLastUsed() {
         return entry.getLastUsed();
      }

      @Override
      public boolean isExpired(long now) {
         return entry.isExpired(now);
      }

      @Override
      public boolean isExpired() {
         return entry.isExpired();
      }

      @Override
      public OffHeapInternalCacheValue toInternalCacheValue() {
         return new OffHeapCoreImmutables.OffHeapImmutableInternalCacheValue(this);
      }

      @Override
      public void touch() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void touch(long currentTimeMillis) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean undelete(boolean doUndelete) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void reincarnate() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void reincarnate(long now) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setMetadata(OffHeapMetadata metadata) {
         throw new UnsupportedOperationException();
      }

      @Override
      public long getLifespan() {
         return entry.getLifespan();
      }

      @Override
      public long getMaxIdle() {
         return entry.getMaxIdle();
      }

      @Override
      public boolean skipLookup() {
         return false;
      }

      @Override
      public boolean isChanged() {
         return entry.isChanged();
      }

      @Override
      public boolean isCreated() {
         return entry.isCreated();
      }

      @Override
      public boolean isNull() {
         return entry.isNull();
      }

      @Override
      public boolean isRemoved() {
         return entry.isRemoved();
      }

      @Override
      public boolean isEvicted() {
         return entry.isEvicted();
      }

      @Override
      public boolean isValid() {
         return entry.isValid();
      }

      @Override
      public boolean isLoaded() {
         return entry.isLoaded();
      }

      @Override
      public void rollback() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setCreated(boolean created) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setRemoved(boolean removed) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setChanged(boolean changed) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setEvicted(boolean evicted) {
         entry.setEvicted(evicted);
      }

      @Override
      public void setValid(boolean valid) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setLoaded(boolean loaded) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setSkipLookup(boolean skipLookup) {
         throw new UnsupportedOperationException();
      }

      @Override
      public OffHeapMetadata getMetadata() {
         return entry.getMetadata();
      }

      @Override
      public OffHeapInternalCacheEntry clone() {
         return new OffHeapImmutableInternalCacheEntry(entry.clone());
      }
   }

   private static class OffHeapImmutableInternalCacheValue implements OffHeapInternalCacheValue, Immutable {
      private final OffHeapImmutableInternalCacheEntry entry;

      OffHeapImmutableInternalCacheValue(OffHeapImmutableInternalCacheEntry entry) {
         this.entry = entry;
      }

      @Override
      public boolean canExpire() {
         return entry.canExpire();
      }

      @Override
      public long getCreated() {
         return entry.getCreated();
      }

      @Override
      public long getLastUsed() {
         return entry.getLastUsed();
      }

      @Override
      public long getLifespan() {
         return entry.getLifespan();
      }

      @Override
      public long getMaxIdle() {
         return entry.getMaxIdle();
      }

      @Override
      public Object getValue() {
         return entry.getValue();
      }

      @Override
      public boolean isExpired(long now) {
         return entry.isExpired(now);
      }

      @Override
      public boolean isExpired() {
         return entry.isExpired();
      }

      @Override
      public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
         return entry;
      }

      @Override
      public long getExpiryTime() {
         return entry.toInternalCacheValue().getExpiryTime();
      }

      @Override
      public OffHeapMetadata getMetadata() {
         return entry.getMetadata();
      }
   }
}
