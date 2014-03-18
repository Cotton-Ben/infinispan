package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.commons.util.concurrent.OffHeapUtil;
import org.infinispan.offheap.container.entries.OffHeapAbstractInternalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheValue;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * A cache entry that is transient, i.e., it can be considered expired after
 * a period of not being used, and {@link OffHeapMetadataAware}
 *
 * @author Galder Zamarreño
 * @since 5.3
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataTransientCacheEntry extends OffHeapAbstractInternalCacheEntry implements OffHeapMetadataAware {

   protected Object value;
   protected OffHeapMetadata metadata;
   protected long lastUsed;

   public OffHeapMetadataTransientCacheEntry(Object key, Object value, OffHeapMetadata metadata, long lastUsed) {
      super(key);
      this.value = value;
      this.metadata = metadata;
      this.lastUsed = lastUsed;
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
   public final void touch() {
      touch(System.currentTimeMillis());
   }

   @Override
   public final void touch(long currentTimeMillis) {
      lastUsed = currentTimeMillis;
   }


   @Override
   public final void reincarnate() {
      // no-op
   }

   @Override
   public void reincarnate(long now) {
      //no-op
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransient(metadata.maxIdle(), lastUsed, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public long getCreated() {
      return -1;
   }

   @Override
   public final long getLastUsed() {
      return lastUsed;
   }

   @Override
   public long getLifespan() {
      return -1;
   }

   @Override
   public long getExpiryTime() {
      long maxIdle = metadata.maxIdle();
      return maxIdle > -1 ? lastUsed + maxIdle : -1;
   }

   @Override
   public final long getMaxIdle() {
      return metadata.maxIdle();
   }

   @Override
   public OffHeapInternalCacheValue toInternalCacheValue() {
      return new OffHeapMetadataTransientCacheValue(value, metadata, lastUsed);
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(OffHeapMetadata metadata) {
      this.metadata = metadata;
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapMetadataTransientCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMetadataTransientCacheEntry ice) throws IOException {
         output.writeObject(ice.key);
         output.writeObject(ice.value);
         output.writeObject(ice.metadata);
         UnsignedNumeric.writeUnsignedLong(output, ice.lastUsed);
      }

      @Override
      public OffHeapMetadataTransientCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         OffHeapMetadata metadata = (OffHeapMetadata) input.readObject();
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         return new OffHeapMetadataTransientCacheEntry(k, v, metadata, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_TRANSIENT_ENTRY;
      }

      @Override
      public Set<Class<? extends OffHeapMetadataTransientCacheEntry>> getTypeClasses() {
         return OffHeapUtil.<Class<? extends OffHeapMetadataTransientCacheEntry>>asSet(OffHeapMetadataTransientCacheEntry.class);
      }
   }
}
