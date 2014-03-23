package org.infinispan.offheap.container.versioning;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.InequalVersionComparisonResult;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.marshall.core.Ids;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

/**
 * Numeric version
 *
 * @author Galder Zamarreño
 * @since 5.3
 */
public class OffHeapNumericVersion implements IncrementableEntryVersion {

   private final long version;

   public OffHeapNumericVersion(long version) {
      this.version = version;
   }

   public long getVersion() {
      return version;
   }

   @Override
   public InequalVersionComparisonResult compareTo(EntryVersion other) {
      if (other instanceof NumericVersion) {
         OffHeapNumericVersion otherVersion = (OffHeapNumericVersion) other;
         if (version < otherVersion.version)
            return InequalVersionComparisonResult.BEFORE;
         else if (version > otherVersion.version)
            return InequalVersionComparisonResult.AFTER;
         else
            return InequalVersionComparisonResult.EQUAL;
      }

      throw new IllegalArgumentException(
            "Unable to compare other types: " + other.getClass().getName()
      );
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      OffHeapNumericVersion that = (OffHeapNumericVersion) o;

      if (version != that.version) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return (int) (version ^ (version >>> 32));
   }

   @Override
   public String toString() {
      return "NumericVersion{" +
            "version=" + version +
            '}';
   }

   public static class OffHeapExternalizer extends AbstractExternalizer<OffHeapNumericVersion> {

      @Override
      public Set<Class<? extends OffHeapNumericVersion>> getTypeClasses() {
         return Collections.<Class<? extends OffHeapNumericVersion>>singleton(OffHeapNumericVersion.class);
      }

      @Override
      public void writeObject(ObjectOutput output, OffHeapNumericVersion object) throws IOException {
         output.writeLong(object.version);
      }

      @Override
      public OffHeapNumericVersion readObject(ObjectInput input) throws IOException {
         return new OffHeapNumericVersion(input.readLong());
      }

      @Override
      public Integer getId() {
         return Ids.NUMERIC_VERSION;
      }

   }

}
