package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.OffHeapImmortalCacheValue;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import org.infinispan.offheap.metadata.OffHeapMetadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * A mortal cache value, to correspond with
 * {@link OffHeapMetadataMortalCacheEntry}
 *
 * @author Galder Zamarreño
 * @since 5.1
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataMortalCacheValue
                                            extends OffHeapImmortalCacheValue
                                            implements OffHeapMetadataAware {

   OffHeapMetadata metadata;
   long created;

   public OffHeapMetadataMortalCacheValue(Object value, OffHeapMetadata metadata, long created) {
      super(value);
      this.metadata = metadata;
      this.created = created;
   }

   @Override
   public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapMetadataMortalCacheEntry(key, value, metadata, created);
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return metadata;
   }


    @Override
   public void setMetadata(OffHeapMetadata metadata) {
      this.metadata = metadata;
   }

   @Override
   public final long getCreated() {
      return created;
   }

   @Override
   public final long getLifespan() {
      return metadata.lifespan();
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredMortal(metadata.lifespan(), created, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public long getExpiryTime() {
      long lifespan = metadata.lifespan();
      return lifespan > -1 ? created + lifespan : -1;
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   public static class OffHeapExternalizer extends OffHeapAbstractExternalizer<OffHeapMetadataMortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMetadataMortalCacheValue mcv) throws IOException {
         output.writeObject(mcv.value);
         output.writeObject(mcv.metadata);
         UnsignedNumeric.writeUnsignedLong(output, mcv.created);
      }

      @Override
      public OffHeapMetadataMortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         OffHeapMetadata metadata = (OffHeapMetadata) input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         return new OffHeapMetadataMortalCacheValue(v, metadata, created);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_MORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapMetadataMortalCacheValue>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMetadataMortalCacheValue>>asSet(OffHeapMetadataMortalCacheValue.class);
      }
   }

}
