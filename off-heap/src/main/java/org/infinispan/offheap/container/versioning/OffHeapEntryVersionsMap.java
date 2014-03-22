package org.infinispan.offheap.container.versioning;

import org.infinispan.container.versioning.IncrementableEntryVersion;

import java.util.HashMap;

public class OffHeapEntryVersionsMap extends HashMap<Object, IncrementableEntryVersion> {
   public OffHeapEntryVersionsMap merge(OffHeapEntryVersionsMap updatedVersions) {
      if (updatedVersions != null && !updatedVersions.isEmpty()) {
         updatedVersions.putAll(this);
         return updatedVersions;
      } else {
         return this;
      }
   }
}
