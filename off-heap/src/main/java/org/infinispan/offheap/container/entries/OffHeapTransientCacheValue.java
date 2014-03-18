package org.infinispan.offheap.container.entries;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * A transient cache value, to correspond with {@link OffHeapTransientCacheEntry}
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class OffHeapTransientCacheValue extends OffHeapImmortalCacheValue {
   protected long maxIdle = -1;
   protected long lastUsed;

   public OffHeapTransientCacheValue(Object value, long maxIdle, long lastUsed) {
      super(value);
      this.maxIdle = maxIdle;
      this.lastUsed = lastUsed;
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
   public final boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransient(maxIdle, lastUsed, now);
   }

   @Override
   public final boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public boolean canExpire() {
      return true;
   }

   @Override
   public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapTransientCacheEntry(key, value, maxIdle, lastUsed);
   }

   @Override
   public long getExpiryTime() {
      return maxIdle > -1 ? lastUsed + maxIdle : -1;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof OffHeapTransientCacheValue)) return false;
      if (!super.equals(o)) return false;

      OffHeapTransientCacheValue that = (OffHeapTransientCacheValue) o;

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
      return "TransientCacheValue{" +
            "maxIdle=" + maxIdle +
            ", lastUsed=" + lastUsed +
            "} " + super.toString();
   }

   @Override
   public OffHeapTransientCacheValue clone() {
      return (OffHeapTransientCacheValue) super.clone();
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapTransientCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapTransientCacheValue tcv) throws IOException {
         output.writeObject(tcv.value);
         UnsignedNumeric.writeUnsignedLong(output, tcv.lastUsed);
         output.writeLong(tcv.maxIdle); // could be negative so should not use unsigned longs
      }

      @Override
      public OffHeapTransientCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         Long maxIdle = input.readLong();
         return new OffHeapTransientCacheValue(v, maxIdle, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.TRANSIENT_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapTransientCacheValue>> getTypeClasses() {
         return Util.<Class<? extends OffHeapTransientCacheValue>>asSet(OffHeapTransientCacheValue.class);
      }
   }
}
