package org.infinispan.offheap.metadata;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.offheap.commons.util.concurrent.OffHeapUtil;
import org.infinispan.offheap.container.versioning.OffHeapEntryVersion;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static java.lang.Math.min;

/**
 * @author Mircea Markus
 * @since 6.0
 */
public class OffHeapInternalMetadataImpl implements OffHeapInternalMetadata {
   private final OffHeapMetadata actual;
   private final long created;
   private final long lastUsed;

   public OffHeapInternalMetadataImpl() {
      //required by the AZUL VM in order to run the tests
      this(null, -1, -1);
   }

   public OffHeapInternalMetadataImpl(InternalCacheEntry ice) {
      this((OffHeapMetadata) ice.getMetadata(), ice.getCreated(), ice.getLastUsed());
   }

   public OffHeapInternalMetadataImpl(OffHeapMetadata actual, long created, long lastUsed) {
      this.actual = extractMetadata(actual);
      this.created = created;
      this.lastUsed = lastUsed;
   }

   @Override
   public long lifespan() {
      return actual.lifespan();
   }

   @Override
   public long maxIdle() {
      return actual.maxIdle();
   }

   @Override
   public OffHeapEntryVersion version() {
      return actual.version();
   }

   @Override
   public OffHeapEmbeddedMetadata.OffHeapBuilder builder() {
      return actual.builder();
   }

   @Override
   public long created() {
      return created;
   }

   @Override
   public long lastUsed() {
      return lastUsed;
   }

   public OffHeapMetadata actual() {
      return actual;
   }

   @Override
   public long expiryTime() {
      long lifespan = actual.lifespan();
      long lset = lifespan > -1 ? created + lifespan : -1;
      long maxIdle = actual.maxIdle();
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }

   @Override
   public boolean isExpired(long now) {
      long expiry = expiryTime();
      return expiry > 0 && expiry <= now;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof OffHeapInternalMetadataImpl)) return false;

      OffHeapInternalMetadataImpl that = (OffHeapInternalMetadataImpl) o;

      if (created != that.created) return false;
      if (lastUsed != that.lastUsed) return false;
      if (actual != null ? !actual.equals(that.actual) : that.actual != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = actual != null ? actual.hashCode() : 0;
      result = 31 * result + (int) (created ^ (created >>> 32));
      result = 31 * result + (int) (lastUsed ^ (lastUsed >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "InternalMetadataImpl{" +
            "actual=" + actual +
            ", created=" + created +
            ", lastUsed=" + lastUsed +
            '}';
   }

   private static OffHeapMetadata extractMetadata(OffHeapMetadata metadata) {
      OffHeapMetadata toCheck = metadata;
      while (toCheck != null) {
         if (toCheck instanceof OffHeapInternalMetadataImpl) {
            toCheck = ((OffHeapInternalMetadataImpl) toCheck).actual();
         } else {
            break;
         }
      }
      return toCheck;
   }

   public static class OffHeapExternalizer extends OffHeapAbstractExternalizer<OffHeapInternalMetadataImpl> {

      private static final long serialVersionUID = -5291318076267612501L;

      public OffHeapExternalizer() {
      }

      @Override
      public void writeObject(ObjectOutput output, OffHeapInternalMetadataImpl b) throws IOException {
         output.writeLong(b.created);
         output.writeLong(b.lastUsed);
         output.writeObject(b.actual);
      }

      @Override
      public OffHeapInternalMetadataImpl readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         long created = input.readLong();
         long lastUsed = input.readLong();
         OffHeapMetadata actual = (OffHeapMetadata) input.readObject();
         return new OffHeapInternalMetadataImpl(actual, created, lastUsed);
      }

      @Override
      public Integer getId() {
         return Ids.INTERNAL_METADATA_ID;
      }

      @Override
      @SuppressWarnings("unchecked")
      public Set<Class<? extends OffHeapInternalMetadataImpl>> getTypeClasses() {
         return OffHeapUtil.<Class<? extends OffHeapInternalMetadataImpl>>asSet(OffHeapInternalMetadataImpl.class);
      }
   }
}
