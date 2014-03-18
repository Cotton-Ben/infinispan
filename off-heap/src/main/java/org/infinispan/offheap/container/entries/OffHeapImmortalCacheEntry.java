package org.infinispan.offheap.container.entries;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.AbstractInternalCacheEntry;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static org.infinispan.commons.util.Util.toStr;

/**
 * A cache entry that is immortal/cannot expire
 *
 * @author Manik Surtani
 * @since 4.0
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapImmortalCacheEntry extends OffHeapAbstractInternalCacheEntry {

   public Object value;

   public OffHeapImmortalCacheEntry(Object key, Object value) {
      super(key);
      this.value = value;
   }

   @Override
   public final boolean isExpired(long now) {
      return false;
   }

   @Override
   public final boolean isExpired() {
      return false;
   }

   @Override
   public final boolean canExpire() {
      return false;
   }

   @Override
   public final long getCreated() {
      return -1;
   }

   @Override
   public final long getLastUsed() {
      return -1;
   }

   @Override
   public final long getLifespan() {
      return -1;
   }

   @Override
   public final long getMaxIdle() {
      return -1;
   }

   @Override
   public final long getExpiryTime() {
      return -1;
   }

   @Override
   public final void touch() {
      // no-op
   }

   @Override
   public void touch(long currentTimeMillis) {
      // no-op
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
   public OffHeapInternalCacheValue toInternalCacheValue() {
      return new OffHeapImmortalCacheValue(value);
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
   public OffHeapMetadata getMetadata() {
      return new OffHeapEmbeddedMetadata.OffHeapBuilder().build();
   }

   @Override
   public void setMetadata(OffHeapMetadata metadata) {
      throw new IllegalStateException(
            "Metadata cannot be set on immortal entries. They need to be recreated via the entry factory.");
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      OffHeapImmortalCacheEntry that = (OffHeapImmortalCacheEntry) o;

      if (key != null ? !key.equals(that.key) : that.key != null) return false;
      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = key != null ? key.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      return result;
   }

   @Override
   public OffHeapImmortalCacheEntry clone() {
      return (OffHeapImmortalCacheEntry) super.clone();
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapImmortalCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapImmortalCacheEntry ice) throws IOException {
         output.writeObject(ice.key);
         output.writeObject(ice.value);
      }

      @Override
      public OffHeapImmortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         return new OffHeapImmortalCacheEntry(k, v);
      }

      @Override
      public Integer getId() {
         return Ids.IMMORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends OffHeapImmortalCacheEntry>> getTypeClasses() {
         return Util.<Class<? extends OffHeapImmortalCacheEntry>>asSet(OffHeapImmortalCacheEntry.class);
      }
   }

   @Override
   public String toString() {
      return "ImmortalCacheEntry{" +
            "key=" + toStr(key) +
            ", value=" + toStr(value) +
            "}";
   }

}
