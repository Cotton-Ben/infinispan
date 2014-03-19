package org.infinispan.offheap.container.versioning;

import net.jcip.annotations.Immutable;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.marshall.core.Ids;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

/**
 * A simple versioning scheme that is cluster-aware
 *
 * @author Manik Surtani
 * @since 5.1
 */
@Immutable
public class OffHeapSimpleClusteredVersion implements OffHeapIncrementableEntryVersion {

   /**
    * The cache topology id in which it was first created.
    */
   private final int topologyId;

   final long version;

   public OffHeapSimpleClusteredVersion(int topologyId, long version) {
      this.version = version;
      this.topologyId = topologyId;
   }

   @Override
   public OffHeapInequalVersionComparisonResult compareTo(OffHeapEntryVersion other) {
      if (other instanceof OffHeapSimpleClusteredVersion) {
         OffHeapSimpleClusteredVersion otherVersion = (OffHeapSimpleClusteredVersion) other;

         if (topologyId > otherVersion.topologyId)
            return OffHeapInequalVersionComparisonResult.AFTER;
         if (topologyId < otherVersion.topologyId)
            return OffHeapInequalVersionComparisonResult.BEFORE;

         if (version > otherVersion.version)
            return OffHeapInequalVersionComparisonResult.AFTER;
         if (version < otherVersion.version)
            return OffHeapInequalVersionComparisonResult.BEFORE;

         return OffHeapInequalVersionComparisonResult.EQUAL;
      } else {
         throw new IllegalArgumentException("I only know how to deal with SimpleClusteredVersions, not " + other.getClass().getName());
      }
   }

   @Override
   public String toString() {
      return "SimpleClusteredVersion{" +
            "topologyId=" + topologyId +
            ", version=" + version +
            '}';
   }

   public static class Externalizer extends AbstractExternalizer<OffHeapSimpleClusteredVersion> {

      @Override
      public void writeObject(ObjectOutput output, OffHeapSimpleClusteredVersion ch) throws IOException {
         output.writeInt(ch.topologyId);
         output.writeLong(ch.version);
      }

      @Override
      @SuppressWarnings("unchecked")
      public OffHeapSimpleClusteredVersion readObject(ObjectInput unmarshaller) throws IOException, ClassNotFoundException {
         int topologyId = unmarshaller.readInt();
         long version = unmarshaller.readLong();
         return new OffHeapSimpleClusteredVersion(topologyId, version);
      }

      @Override
      public Integer getId() {
         return Ids.SIMPLE_CLUSTERED_VERSION;
      }

      @Override
      public Set<Class<? extends OffHeapSimpleClusteredVersion>> getTypeClasses() {
         return Collections.<Class<? extends OffHeapSimpleClusteredVersion>>singleton(OffHeapSimpleClusteredVersion.class);
      }
   }
}
