package org.infinispan.offheap.container.entries;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static java.lang.Math.min;
import static org.infinispan.commons.util.Util.toStr;

/**
 * A cache entry that is both transient and mortal.
 *
 * @author Manik Surtani
 * @since 4.0
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapTransientMortalCacheEntry extends OffHeapAbstractInternalCacheEntry {

   protected Object value;
   protected long maxIdle = -1;
   protected long lastUsed;
   protected long lifespan = -1;
   protected long created;

   public OffHeapTransientMortalCacheEntry(Object key, Object value, long maxIdle, long lifespan, long currentTimeMillis) {
      this(key, value, maxIdle, lifespan, currentTimeMillis, currentTimeMillis);
   }

   public OffHeapTransientMortalCacheEntry(Object key, Object value, long maxIdle, long lifespan, long lastUsed, long created) {
      super(key);
      this.value = value;
      this.maxIdle = maxIdle;
      this.lifespan = lifespan;
      this.created = created;
      this.lastUsed = lastUsed;
   }

   public void setLifespan(long lifespan) {
      this.lifespan = lifespan;
   }

   public void setMaxIdle(long maxIdle) {
      this.maxIdle = maxIdle;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public long getLifespan() {
      return lifespan;
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public long getCreated() {
      return created;
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransientMortal(maxIdle, lastUsed, lifespan, created, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public final long getExpiryTime() {
      long lset = lifespan > -1 ? created + lifespan : -1;
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }


   @Override
   public OffHeapInternalCacheValue toInternalCacheValue() {
      return new OffHeapTransientMortalCacheValue(value, created, lifespan, maxIdle, lastUsed);
   }

   @Override
   public long getLastUsed() {
      return lastUsed;
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
      reincarnate(System.currentTimeMillis());
   }

   @Override
   public void reincarnate(long now) {
      this.created = now;
   }

   @Override
   public long getMaxIdle() {
      return maxIdle;
   }

   @Override
   public Object setValue(Object value) {
      return this.value = value;
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return new OffHeapEmbeddedMetadata.OffHeapBuilder()
            .lifespan(lifespan)
            .maxIdle(maxIdle).build();
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

      OffHeapTransientMortalCacheEntry that = (OffHeapTransientMortalCacheEntry) o;

      if (created != that.created) return false;
      if (lifespan != that.lifespan) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (int) (created ^ (created >>> 32));
      result = 31 * result + (int) (lifespan ^ (lifespan >>> 32));
      return result;
   }

   @Override
   public OffHeapTransientMortalCacheEntry clone() {
      return (OffHeapTransientMortalCacheEntry) super.clone();
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "{" +
            "key=" + toStr(key) +
            ", value=" + toStr(value) +
            "}";
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapTransientMortalCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapTransientMortalCacheEntry entry) throws IOException {
         output.writeObject(entry.key);
         output.writeObject(entry.value);
         UnsignedNumeric.writeUnsignedLong(output, entry.created);
         output.writeLong(entry.lifespan); // could be negative so should not use unsigned longs
         UnsignedNumeric.writeUnsignedLong(output, entry.lastUsed);
         output.writeLong(entry.maxIdle); // could be negative so should not use unsigned longs
      }

      @Override
      public OffHeapTransientMortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         Long lifespan = input.readLong();
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         Long maxIdle = input.readLong();
         return new OffHeapTransientMortalCacheEntry(k, v, maxIdle, lifespan, lastUsed, created);
      }

      @Override
      public Integer getId() {
         return Ids.TRANSIENT_MORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends OffHeapTransientMortalCacheEntry>> getTypeClasses() {
         return Util.<Class<? extends OffHeapTransientMortalCacheEntry>>asSet(OffHeapTransientMortalCacheEntry.class);
      }
   }
}

