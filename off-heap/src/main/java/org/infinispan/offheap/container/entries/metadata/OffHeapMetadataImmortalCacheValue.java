package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.entries.OffHeapImmortalCacheValue;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import org.infinispan.offheap.metadata.OffHeapMetadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static org.infinispan.commons.util.Util.toStr;

/**
 * A form of {@link org.infinispan.offheap.container.entries.OffHeapImmortalCacheValue} that
 * is {@link OffHeapMetadataAware}
 *
 * @author Galder Zamarreño
 * @since 5.3
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataImmortalCacheValue extends OffHeapImmortalCacheValue implements OffHeapMetadataAware {

   OffHeapMetadata metadata;

   public OffHeapMetadataImmortalCacheValue(Object value, OffHeapMetadata metadata) {
      super(value);
      this.metadata = metadata;
   }

   @Override
   public OffHeapInternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapMetadataImmortalCacheEntry(key, value, metadata);
   }

   @Override
   public OffHeapMetadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(OffHeapMetadata _metadata) {
      this.metadata = _metadata;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " {" +
            "value=" + toStr(value) +
            ", metadata=" + metadata +
            '}';
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapMetadataImmortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, OffHeapMetadataImmortalCacheValue icv) throws IOException {
         output.writeObject(icv.value);
         output.writeObject(icv.metadata);
      }

      @Override
      public OffHeapMetadataImmortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         OffHeapMetadata metadata = (OffHeapMetadata) input.readObject();
         return new OffHeapMetadataImmortalCacheValue(v, metadata);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_IMMORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends OffHeapMetadataImmortalCacheValue>> getTypeClasses() {
         return Util.<Class<? extends OffHeapMetadataImmortalCacheValue>>asSet(OffHeapMetadataImmortalCacheValue.class);
      }
   }

}
