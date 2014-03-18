package org.infinispan.offheap.container.entries;

import net.openhft.lang.io.Bytes;
import net.openhft.lang.io.serialization.BytesMarshallable;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static org.infinispan.commons.util.Util.toStr;

/**
 * A cache entry that is mortal.  I.e., has a lifespan.
 *
 * @author Manik Surtani
 * @since 4.0
 *
 * @author ben.cotton@jpmorgan.com  (OffHeap OpenHFT integration)
 *
 */
public class OffHeapMortalCacheEntry
                                    extends OffHeapAbstractInternalCacheEntry
                                    implements BytesMarshallable {

   protected Object value;
   protected long lifespan = -1;
   protected long created;

   public OffHeapMortalCacheEntry(Object key, Object value, long lifespan, long created) {
      super(key);
      this.value = value;
      this.lifespan = lifespan;
      this.created = created;
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
   public final boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredMortal(lifespan, created, now);
   }

   @Override
   public final boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   public void setLifespan(long lifespan) {
      this.lifespan = lifespan;
   }

   @Override
   public final long getCreated() {
      return created;
   }

   @Override
   public final long getLastUsed() {
      return -1;
   }

   @Override
   public final long getLifespan() {
      return lifespan;
   }

   @Override
   public final long getMaxIdle() {
      return -1;
   }

   @Override
   public final long getExpiryTime() {
      return lifespan > -1 ? created + lifespan : -1;
   }

   @Override
   public final void touch() {
      // no-op
   }

   @Override
   public final void touch(long currentTimeMillis) {
      // no-op
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
   public OffHeapInternalCacheValue toInternalCacheValue() {
      return new OffHeapMortalCacheValue(value, created, lifespan);
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(lifespan).build();
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

     OffHeapMortalCacheEntry that = (OffHeapMortalCacheEntry) o;

      if (key != null ? !key.equals(that.key) : that.key != null) return false;
      if (value != null ? !value.equals(that.value) : that.value != null)
         return false;
      if (created != that.created) return false;
      return lifespan == that.lifespan;
   }

   @Override
   public int hashCode() {
      int result = key != null ? key.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (int) (created ^ (created >>> 32));
      result = 31 * result + (int) (lifespan ^ (lifespan >>> 32));
      return result;
   }

   @Override
   public OffHeapMortalCacheEntry clone() {
      return (OffHeapMortalCacheEntry) super.clone();
   }

    @Override
    public void readMarshallable(@NotNull Bytes bytes) throws IllegalStateException {

    }

    @Override
    public void writeMarshallable(@NotNull Bytes bytes) {

    }

    public static class Externalizer extends AbstractExternalizer<OffHeapMortalCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMortalCacheEntry mce) throws IOException {
         output.writeObject(mce.key);
         output.writeObject(mce.value);
         UnsignedNumeric.writeUnsignedLong(output, mce.created);
         output.writeLong(mce.lifespan); // could be negative so should not use unsigned longs
      }

      @Override
      public OffHeapMortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         Long lifespan = input.readLong();
         return new OffHeapMortalCacheEntry(k, v, lifespan, created);
      }

      @Override
      public Integer getId() {
         return Ids.MORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends OffHeapMortalCacheEntry>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMortalCacheEntry>>asSet(OffHeapMortalCacheEntry.class);
      }
   }

   @Override
   public String toString() {
      return "MortalCacheEntry{" +
            "key=" + toStr(key) +
            ", value=" + toStr(value) +
            "}";
   }
}
