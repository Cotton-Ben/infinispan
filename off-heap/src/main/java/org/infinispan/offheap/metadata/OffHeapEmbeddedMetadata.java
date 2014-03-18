package org.infinispan.offheap.metadata;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.offheap.container.versioning.OffHeapEntryVersion;
import org.jboss.marshalling.util.IdentityIntMap;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Metadata class for embedded caches.
 *
 * @author Galder Zamarreño
 * @since 5.3
 */
public class OffHeapEmbeddedMetadata implements OffHeapMetadata {

   final OffHeapEntryVersion version;

   private OffHeapEmbeddedMetadata(OffHeapEntryVersion version) {
      this.version = version;
   }

   @Override
   public long lifespan() {
      return -1;
   }

   @Override
   public long maxIdle() {
      return -1;
   }

   @Override
   public OffHeapEntryVersion version() {
      return version;
   }

   @Override
   public OffHeapEmbeddedMetadata.OffHeapBuilder builder() {
      return new OffHeapEmbeddedMetadata.OffHeapBuilder().version(version);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      OffHeapEmbeddedMetadata that = (OffHeapEmbeddedMetadata) o;

      if (version != null ? !version.equals(that.version) : that.version != null)
         return false;

      return true;
   }

   @Override
   public int hashCode() {
      return version != null ? version.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "EmbeddedMetadata{" +
            "version=" + version +
            '}';
   }

   public static class OffHeapBuilder implements OffHeapMetadata.Builder {

      protected long lifespan = -1;
      protected TimeUnit lifespanUnit = TimeUnit.MILLISECONDS;
      protected long maxIdle = -1;
      protected TimeUnit maxIdleUnit = TimeUnit.MILLISECONDS;
      protected OffHeapEntryVersion version;

      @Override
      public OffHeapEmbeddedMetadata.OffHeapBuilder lifespan(long time, TimeUnit unit) {
         lifespan = time;
         lifespanUnit = unit;
         return this;
      }

      @Override
      public OffHeapEmbeddedMetadata.OffHeapBuilder lifespan(long time) {
         return lifespan(time, TimeUnit.MILLISECONDS);
      }

      @Override
      public OffHeapEmbeddedMetadata.OffHeapBuilder maxIdle(long time, TimeUnit unit) {
         maxIdle = time;
         maxIdleUnit = unit;
         return this;
      }

      @Override
      public OffHeapEmbeddedMetadata.OffHeapBuilder maxIdle(long time) {
         return maxIdle(time, TimeUnit.MILLISECONDS);
      }

      @Override
      public OffHeapEmbeddedMetadata.OffHeapBuilder version(OffHeapEntryVersion version) {
         this.version = version;
         return this;
      }

      @Override
      public OffHeapMetadata build() {
         if (lifespan < 0 && maxIdle < 0)
            return new OffHeapEmbeddedMetadata(version);
         else
            return new OffHeapEmbeddedExpirableMetadata(
               lifespan, lifespanUnit, maxIdle, maxIdleUnit, version);
      }

   }

   private static class OffHeapEmbeddedExpirableMetadata extends OffHeapEmbeddedMetadata {

      private final long lifespan;
      private final long maxIdle;

      private OffHeapEmbeddedExpirableMetadata(
            long lifespan, TimeUnit lifespanUnit,
            long maxIdle, TimeUnit maxIdleUnit, OffHeapEntryVersion version) {
         super(version);
         this.lifespan = lifespanUnit.toMillis(lifespan);
         this.maxIdle = maxIdleUnit.toMillis(maxIdle);
      }

      @Override
      public long lifespan() {
         return lifespan;
      }

      @Override
      public long maxIdle() {
         return maxIdle;
      }

      @Override
      public  OffHeapEmbeddedMetadata.OffHeapBuilder builder() {
         return new OffHeapBuilder()
               .lifespan(lifespan).maxIdle(lifespan).version(version);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         if (!super.equals(o)) return false;

         OffHeapEmbeddedExpirableMetadata that = (OffHeapEmbeddedExpirableMetadata) o;

         if (lifespan != that.lifespan) return false;
         if (maxIdle != that.maxIdle) return false;

         return true;
      }

      @Override
      public int hashCode() {
         int result = super.hashCode();
         result = 31 * result + (int) (lifespan ^ (lifespan >>> 32));
         result = 31 * result + (int) (maxIdle ^ (maxIdle >>> 32));
         return result;
      }

      @Override
      public String toString() {
         return "EmbeddedExpirableMetadata{" +
               "lifespan=" + lifespan +
               ", maxIdle=" + maxIdle +
               ", version=" + version +
               '}';
      }
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapEmbeddedMetadata> {

      private static final int IMMORTAL = 0;
      private static final int EXPIRABLE = 1;
      private final IdentityIntMap<Class<?>> numbers = new IdentityIntMap<Class<?>>(2);

      public Externalizer() {
         numbers.put(OffHeapEmbeddedMetadata.class, IMMORTAL);
         numbers.put(OffHeapEmbeddedExpirableMetadata.class, EXPIRABLE);
      }

      @Override
      @SuppressWarnings("unchecked")
      public Set<Class<? extends OffHeapEmbeddedMetadata>> getTypeClasses() {
         return Util.asSet(OffHeapEmbeddedMetadata.class, OffHeapEmbeddedExpirableMetadata.class);
      }

      @Override
      public Integer getId() {
         return Ids.EMBEDDED_METADATA;
      }

      @Override
      public void writeObject(ObjectOutput output, OffHeapEmbeddedMetadata object) throws IOException {
         int number = numbers.get(object.getClass(), -1);
         output.write(number);
         switch (number) {
            case EXPIRABLE:
               output.writeLong(object.lifespan());
               output.writeLong(object.maxIdle());
               break;
         }

         output.writeObject(object.version());
      }

      @Override
      public OffHeapEmbeddedMetadata readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         int number = input.readUnsignedByte();
         switch (number) {
            case IMMORTAL:
               return new OffHeapEmbeddedMetadata(
                     (OffHeapEntryVersion) input.readObject()
               );
            case EXPIRABLE:
               long lifespan = input.readLong();
               long maxIdle = input.readLong();
               OffHeapEntryVersion version = (OffHeapEntryVersion) input.readObject();
               return new OffHeapEmbeddedExpirableMetadata(
                     lifespan, TimeUnit.MILLISECONDS,
                     maxIdle, TimeUnit.MILLISECONDS, version);
            default:
               throw new IllegalStateException("Unknown metadata type " + number);
         }
      }

   }

}
