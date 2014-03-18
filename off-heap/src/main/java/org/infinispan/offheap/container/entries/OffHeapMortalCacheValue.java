package org.infinispan.offheap.container.entries;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.MortalCacheValue;
import org.infinispan.marshall.core.Ids;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * A mortal cache value, to correspond with
 * {@link org.infinispan.offheap.container.entries.OffHeapMortalCacheEntry}
 *
 * @author Manik Surtani
 * @since 4.0
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMortalCacheValue extends OffHeapImmortalCacheValue {

   protected long created;
   protected long lifespan = -1;

   public OffHeapMortalCacheValue(Object value, long created, long lifespan) {
      super(value);
      this.created = created;
      this.lifespan = lifespan;
   }

   @Override
   public final long getCreated() {
      return created;
   }

   public final void setCreated(long created) {
      this.created = created;
   }

   @Override
   public final long getLifespan() {
      return lifespan;
   }

   public final void setLifespan(long lifespan) {
      this.lifespan = lifespan;
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredMortal(lifespan, created, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public
   OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapMortalCacheEntry(key, value, lifespan, created);
   }

   @Override
   public long getExpiryTime() {
      return lifespan > -1 ? created + lifespan : -1;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof OffHeapMortalCacheValue)) return false;
      if (!super.equals(o)) return false;

      OffHeapMortalCacheValue that = (OffHeapMortalCacheValue) o;

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
   public String toString() {
      return "MortalCacheValue{" +
            "value=" + value +
            ", lifespan=" + lifespan +
            ", created=" + created +
            "}";
   }

   @Override
   public OffHeapMortalCacheValue clone() {
      return (OffHeapMortalCacheValue) super.clone();
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapMortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMortalCacheValue mcv) throws IOException {
         output.writeObject(mcv.value);
         UnsignedNumeric.writeUnsignedLong(output, mcv.created);
         output.writeLong(mcv.lifespan); // could be negative so should not use unsigned longs
      }

      @Override
      public OffHeapMortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         Long lifespan = input.readLong();
         return new OffHeapMortalCacheValue(v, created, lifespan);
      }

      @Override
      public Integer getId() {
         return Ids.MORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapMortalCacheValue>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMortalCacheValue>>asSet(OffHeapMortalCacheValue.class);
      }
   }
}
