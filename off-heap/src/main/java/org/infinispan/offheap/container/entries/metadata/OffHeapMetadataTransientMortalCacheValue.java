package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static java.lang.Math.min;

/**
 * A form of {@link org.infinispan.offheap.container.entries.OffHeapTransientMortalCacheValue}
 * that is {@link org.infinispan.offheap.container.entries.versioned.OffHeapVersioned}
 *
 * @author Manik Surtani
 * @since 5.1
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataTransientMortalCacheValue extends OffHeapMetadataMortalCacheValue implements OffHeapMetadataAware {

   long lastUsed;

   public OffHeapMetadataTransientMortalCacheValue(Object v, OffHeapMetadata metadata, long created, long lastUsed) {
      super(v, metadata, created);
      this.lastUsed = lastUsed;
   }

   @Override
   public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapMetadataTransientMortalCacheEntry(key, value, metadata, lastUsed, created);
   }

   @Override
   public long getMaxIdle() {
      return metadata.maxIdle();
   }

   @Override
   public long getLastUsed() {
      return lastUsed;
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransientMortal(
              metadata.maxIdle(), lastUsed, metadata.lifespan(), created, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public long getExpiryTime() {
      long lifespan = metadata.lifespan();
      long lset = lifespan > -1 ? created + lifespan : -1;
      long maxIdle = metadata.maxIdle();
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapMetadataTransientMortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMetadataTransientMortalCacheValue value) throws IOException {
         output.writeObject(value.value);
         output.writeObject(value.metadata);
         UnsignedNumeric.writeUnsignedLong(output, value.created);
         UnsignedNumeric.writeUnsignedLong(output, value.lastUsed);
      }

      @Override
      public OffHeapMetadataTransientMortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         OffHeapMetadata metadata = (OffHeapMetadata) input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         return new OffHeapMetadataTransientMortalCacheValue(v, metadata, created, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_TRANSIENT_MORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapMetadataTransientMortalCacheValue>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMetadataTransientMortalCacheValue>>asSet(OffHeapMetadataTransientMortalCacheValue.class);
      }
   }

}
