package org.infinispan.offheap.container.entries;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.TransientCacheEntry;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.infinispan.commons.util.Util.toStr;

/**
 * A cache entry that is transient, i.e., it can be considered expired after a period of not being used.
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class OffHeapTransientCacheEntry extends OffHeapAbstractInternalCacheEntry {

   protected Object value;
   protected long maxIdle = -1;
   protected long lastUsed;

   public OffHeapTransientCacheEntry(Object key, Object value, long maxIdle, long lastUsed) {
      super(key);
      this.value = value;
      this.maxIdle = maxIdle;
      this.lastUsed = lastUsed;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public Object setValue(Object value) {
      return this.value = value;
   }

   @Override
   public final void touch() {
      touch(System.currentTimeMillis());
   }

   @Override
   public final void touch(long currentTimeMillis) {
      this.lastUsed = currentTimeMillis;
   }


   @Override
   public final void reincarnate() {
      // no-op
   }

   @Override
   public void reincarnate(long now) {
      // no-op
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransient(maxIdle, lastUsed, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   public void setMaxIdle(long maxIdle) {
      this.maxIdle = maxIdle;
   }

   @Override
   public long getCreated() {
      return -1;
   }

   @Override
   public final long getLastUsed() {
      return lastUsed;
   }

   @Override
   public long getLifespan() {
      return -1;
   }

   @Override
   public long getExpiryTime() {
      return maxIdle > -1 ? lastUsed + maxIdle : -1;
   }

   @Override
   public final long getMaxIdle() {
      return maxIdle;
   }

   @Override
   public OffHeapInternalCacheValue toInternalCacheValue() {
      return new OffHeapTransientCacheValue(value, maxIdle, lastUsed);
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return new OffHeapEmbeddedMetadata.OffHeapBuilder()
            .maxIdle(maxIdle, TimeUnit.MILLISECONDS).build();
   }

   @Override
   public void setMetadata(OffHeapMetadata metadata) {
      throw new IllegalStateException(
            "Metadata cannot be set on mortal entries. They need to be recreated via the entry factory.");
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      OffHeapTransientCacheEntry that = (OffHeapTransientCacheEntry) o;

      if (key != null ? !key.equals(that.key) : that.key != null) return false;
      if (value != null ? !value.equals(that.value) : that.value != null)
         return false;
      if (lastUsed != that.lastUsed) return false;
      if (maxIdle != that.maxIdle) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = key != null ? key.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (int) (lastUsed ^ (lastUsed >>> 32));
      result = 31 * result + (int) (maxIdle ^ (maxIdle >>> 32));
      return result;
   }

   @Override
   public OffHeapTransientCacheEntry clone() {
      return (OffHeapTransientCacheEntry) super.clone();
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapTransientCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapTransientCacheEntry tce) throws IOException {
         output.writeObject(tce.key);
         output.writeObject(tce.value);
         UnsignedNumeric.writeUnsignedLong(output, tce.lastUsed);
         output.writeLong(tce.maxIdle); // could be negative so should not use unsigned longs
      }

      @Override
      public OffHeapTransientCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         Long maxIdle = input.readLong();
         return new OffHeapTransientCacheEntry(k, v, maxIdle, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.TRANSIENT_ENTRY;
      }

      @Override
      public Set<Class<? extends OffHeapTransientCacheEntry>> getTypeClasses() {
         return Util.<Class<? extends OffHeapTransientCacheEntry>>asSet(OffHeapTransientCacheEntry.class);
      }
   }

   @Override
   public String toString() {
      return "TransientCacheEntry{" +
            "key=" + toStr(key) +
            ", value=" + toStr(value) +
            "}";
   }
}
