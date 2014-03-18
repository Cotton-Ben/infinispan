package org.infinispan.offheap.container.entries;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.TransientMortalCacheValue;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.commons.util.concurrent.OffHeapUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static java.lang.Math.min;

/**
 * A transient, mortal cache value to correspond with {@link OffHeapTransientMortalCacheEntry}
 *
 *  @author ben.cotton@jpmorgan.com
 *  @author dmitry.gordeev@jpmorgan.com
 *  @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapTransientMortalCacheValue extends OffHeapMortalCacheValue {
   protected long maxIdle = -1;
   protected long lastUsed;

   public OffHeapTransientMortalCacheValue(Object value, long created, long lifespan, long maxIdle, long lastUsed) {
      this(value, created, lifespan, maxIdle);
      this.lastUsed = lastUsed;
   }

   public OffHeapTransientMortalCacheValue(Object value, long created, long lifespan, long maxIdle) {
      super(value, created, lifespan);
      this.maxIdle = maxIdle;
   }

   @Override
   public long getMaxIdle() {
      return maxIdle;
   }

   public void setMaxIdle(long maxIdle) {
      this.maxIdle = maxIdle;
   }

   @Override
   public long getLastUsed() {
      return lastUsed;
   }

   public void setLastUsed(long lastUsed) {
      this.lastUsed = lastUsed;
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
   public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapTransientMortalCacheEntry(key, value, maxIdle, lifespan, lastUsed, created);
   }

   @Override
   public long getExpiryTime() {
      long lset = lifespan > -1 ? created + lifespan : -1;
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof TransientMortalCacheValue)) return false;
      if (!super.equals(o)) return false;

      OffHeapTransientMortalCacheValue that = (OffHeapTransientMortalCacheValue) o;

      if (lastUsed != that.lastUsed) return false;
      if (maxIdle != that.maxIdle) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (int) (maxIdle ^ (maxIdle >>> 32));
      result = 31 * result + (int) (lastUsed ^ (lastUsed >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "TransientMortalCacheValue{" +
            "maxIdle=" + maxIdle +
            ", lastUsed=" + lastUsed +
            "} " + super.toString();
   }

   @Override
   public OffHeapTransientMortalCacheValue clone() {
      return (OffHeapTransientMortalCacheValue) super.clone();
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapTransientMortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapTransientMortalCacheValue value) throws IOException {
         output.writeObject(value.value);
         UnsignedNumeric.writeUnsignedLong(output, value.created);
         output.writeLong(value.lifespan); // could be negative so should not use unsigned longs
         UnsignedNumeric.writeUnsignedLong(output, value.lastUsed);
         output.writeLong(value.maxIdle); // could be negative so should not use unsigned longs
      }

      @Override
      public OffHeapTransientMortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         Long lifespan = input.readLong();
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         Long maxIdle = input.readLong();
         return new OffHeapTransientMortalCacheValue(v, created, lifespan, maxIdle, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.TRANSIENT_MORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapTransientMortalCacheValue>> getTypeClasses() {
         return OffHeapUtil.<Class<? extends OffHeapTransientMortalCacheValue>>asSet(OffHeapTransientMortalCacheValue.class);
      }
   }
}
