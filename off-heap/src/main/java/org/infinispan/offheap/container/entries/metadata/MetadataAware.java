package org.infinispan.offheap.container.entries.metadata;

import org.infinispan.metadata.Metadata;

/**
 * OffHeapMetdata aware cache entry.
 *
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public interface MetadataAware {

   /**
    * Get metadata of this cache entry.
    *
    * @return a Metadata instance
    */
   Metadata getMetadata();

   /**
    * Set the metadata in the cache entry.
    *
    * @param metadata to apply to the cache entry
    */
   void setMetadata(Metadata metadata);

}
