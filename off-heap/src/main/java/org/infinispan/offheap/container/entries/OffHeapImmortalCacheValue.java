package org.infinispan.offheap.container.entries;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.offheap.commons.util.concurrent.OffHeapUtil;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * An immortal cache value, to correspond with {@link OffHeapImmortalCacheEntry}
 *
 * @author Manik Surtani
 * @since 4.0
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public class OffHeapImmortalCacheValue implements OffHeapInternalCacheValue, Cloneable {

   public Object value;

   public OffHeapImmortalCacheValue(Object value) {
      this.value = value;
   }

   @Override
   public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return (OffHeapInternalCacheEntry) new OffHeapImmortalCacheEntry(key, value);
   }

   public final Object setValue(Object value) {
      Object old = this.value;
      this.value = value;
      return old;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public boolean isExpired(long now) {
      return false;
   }

   @Override
   public boolean isExpired() {
      return false;
   }

   @Override
   public boolean canExpire() {
      return false;
   }

   @Override
   public long getCreated() {
      return -1;
   }

   @Override
   public long getLastUsed() {
      return -1;
   }

   @Override
   public long getLifespan() {
      return -1;
   }

   @Override
   public long getMaxIdle() {
      return -1;
   }

   @Override
   public long getExpiryTime() {
      return -1;
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return new OffHeapEmbeddedMetadata.OffHeapBuilder()
              .lifespan(getLifespan())
              .maxIdle(getMaxIdle())
              .build();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof OffHeapImmortalCacheValue)) return false;

      ImmortalCacheValue that = (ImmortalCacheValue) o;

      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return value != null ? value.hashCode() : 0;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " {" +
            "value=" + value +
            '}';
   }

   @Override
   public OffHeapImmortalCacheValue clone() {
      try {
         return (OffHeapImmortalCacheValue) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException("Should never happen", e);
      }
   }

   public static class Externalizer extends OffHeapAbstractExternalizer<OffHeapImmortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapImmortalCacheValue icv) throws IOException {
         output.writeObject(icv.value);
      }

      @Override
      public OffHeapImmortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         return new OffHeapImmortalCacheValue(v);
      }

      @Override
      public Integer getId() {
         return Ids.IMMORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapImmortalCacheValue>> getTypeClasses() {
         return OffHeapUtil.<Class<? extends OffHeapImmortalCacheValue>>asSet(
                 OffHeapImmortalCacheValue.class
         );
      }
   }
}
