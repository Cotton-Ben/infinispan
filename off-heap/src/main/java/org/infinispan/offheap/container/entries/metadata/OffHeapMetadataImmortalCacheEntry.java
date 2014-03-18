package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.offheap.container.entries.OffHeapImmortalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheValue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static org.infinispan.commons.util.Util.toStr;

/**
 * A form of {@link org.infinispan.offheap.container.entries.OffHeapImmortalCacheEntry} that
 * is {@link org.infinispan.offheap.container.entries.metadata.OffHeapMetadataAware}
 *
 * @author Galder Zamarreño
 * @since 5.3
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public class OffHeapMetadataImmortalCacheEntry extends OffHeapImmortalCacheEntry implements OffHeapMetadataAware {

   protected OffHeapMetadata _metadata;

   public OffHeapMetadataImmortalCacheEntry(Object key, Object value, OffHeapMetadata metadata) {
      super(key, value);
      this._metadata = metadata;
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return _metadata;
   }

   @Override
   public void setMetadata( OffHeapMetadata metadata) {
      this._metadata = metadata;
   }

   @Override
   public OffHeapInternalCacheValue toInternalCacheValue() {
      return new OffHeapMetadataImmortalCacheValue(value, _metadata);
   }

   @Override
   public String toString() {
      return String.format("MetadataImmortalCacheEntry{key=%s, value=%s, metadata=%s}",
            toStr(key), toStr(value), _metadata);
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapMetadataImmortalCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMetadataImmortalCacheEntry ice) throws IOException {
         output.writeObject(ice.key);
         output.writeObject(ice.value);
         output.writeObject(ice._metadata);
      }

      @Override
      public OffHeapMetadataImmortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         OffHeapMetadata metadata = (OffHeapMetadata) input.readObject();
         return new OffHeapMetadataImmortalCacheEntry(k, v, metadata);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_IMMORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends OffHeapMetadataImmortalCacheEntry>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMetadataImmortalCacheEntry>>asSet(OffHeapMetadataImmortalCacheEntry.class);
      }
   }
}
