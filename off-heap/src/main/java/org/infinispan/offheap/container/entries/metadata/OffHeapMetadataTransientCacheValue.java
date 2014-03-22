package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataAware;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.OffHeapImmortalCacheValue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/*
 * @author Galder Zamarreño
 * @since 5.3
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataTransientCacheValue extends OffHeapImmortalCacheValue implements MetadataAware {

   Metadata metadata;
   long lastUsed;

   public OffHeapMetadataTransientCacheValue(Object value, Metadata metadata, long lastUsed) {
      super(value);
      this.metadata = metadata;
      this.lastUsed = lastUsed;
   }

   @Override
   public InternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapMetadataTransientCacheEntry(key, value, metadata, lastUsed);
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
   public final boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransient(metadata.maxIdle(), lastUsed, now);
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
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   @Override
   public long getExpiryTime() {
      long maxIdle = metadata.maxIdle();
      return maxIdle > -1 ? lastUsed + maxIdle : -1;
   }

   public static class OffHeapExternalizer extends OffHeapAbstractExternalizer<OffHeapMetadataTransientCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMetadataTransientCacheValue tcv) throws IOException {
         output.writeObject(tcv.value);
         output.writeObject(tcv.metadata);
         UnsignedNumeric.writeUnsignedLong(output, tcv.lastUsed);
      }

      @Override
      public OffHeapMetadataTransientCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         Metadata metadata = (Metadata) input.readObject();
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         return new OffHeapMetadataTransientCacheValue(v, metadata, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_TRANSIENT_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapMetadataTransientCacheValue>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMetadataTransientCacheValue>>asSet(OffHeapMetadataTransientCacheValue.class);
      }
   }
}
