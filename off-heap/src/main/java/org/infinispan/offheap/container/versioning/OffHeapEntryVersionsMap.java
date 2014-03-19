package org.infinispan.offheap.container.versioning;

import java.util.HashMap;

public class OffHeapEntryVersionsMap extends HashMap<Object, OffHeapIncrementableEntryVersion> {
   public OffHeapEntryVersionsMap merge(OffHeapEntryVersionsMap updatedVersions) {
      if (updatedVersions != null && !updatedVersions.isEmpty()) {
         updatedVersions.putAll(this);
         return updatedVersions;
      } else {
         return this;
      }
   }
}
