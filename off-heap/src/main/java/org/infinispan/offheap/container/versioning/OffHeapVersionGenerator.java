package org.infinispan.offheap.container.versioning;

/**
 * Generates versions
 *
 * @author Manik Surtani
 * @since 5.1
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public interface OffHeapVersionGenerator {
   /**
    * Generates a new entry version
    * @return a new entry version
    */
   OffHeapIncrementableEntryVersion generateNew();

   OffHeapIncrementableEntryVersion increment(OffHeapIncrementableEntryVersion initialVersion);

   OffHeapIncrementableEntryVersion nonExistingVersion();
}
