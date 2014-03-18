package org.infinispan.offheap.container;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.entries.OffHeapCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheValue;
import org.infinispan.offheap.container.versioning.OffHeapEntryVersion;
import org.infinispan.offheap.metadata.OffHeapMetadata;

/**
 * A factory for {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry}
 * and {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheValue} instances.
 *
 * @author Manik Surtani
 * @since 5.1
 */
@Scope(Scopes.NAMED_CACHE)
public interface OffHeapInternalEntryFactory {

    OffHeapInternalCacheEntry create(Object key, Object value, OffHeapMetadata metadata);

    /**
    * Creates a new {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} instance based on the key, value, version and timestamp/lifespan
    * information reflected in the {@link org.infinispan.offheap.container.entries.OffHeapCacheEntry} instance passed in.
    * @param cacheEntry cache entry to copy
    * @return a new InternalCacheEntry
    */
   OffHeapInternalCacheEntry create(OffHeapCacheEntry cacheEntry);

   /**
    * Creates a new {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} instance based on the version and timestamp/lifespan
    * information reflected in the {@link org.infinispan.offheap.container.entries.OffHeapCacheEntry} instance passed in.  Key and value are both passed in
    * explicitly.
    * @param key key to use
    * @param value value to use
    * @param cacheEntry cache entry to retrieve version and timestamp/lifespan information from
    * @return a new InternalCacheEntry
    */
   OffHeapInternalCacheEntry create(Object key, Object value, OffHeapInternalCacheEntry cacheEntry);

   /**
    * Creates a new {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} instance
    * @param key key to use
    * @param value value to use
    * @param metadata metadata for entry
    * @return a new InternalCacheEntry
    */
   //OffHeapInternalCacheEntry create(Object key, Object value, OffHeapMetadata metadata);

   /**
    * Creates a new {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} instance
    * @param key key to use
    * @param value value to use
    * @param metadata metadata for entry
    * @param lifespan lifespan to use
    * @param maxIdle maxIdle to use
    * @return a new InternalCacheEntry
    */
   //OffHeapInternalCacheEntry create(Object key, Object value, OffHeapMetadata metadata, long lifespan, long maxIdle);

   /**
    * Creates a new {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} instance
    * @param key key to use
    * @param value value to use
    * @param metadata metadata for entry
    * @param created creation timestamp to use
    * @param lifespan lifespan to use
    * @param lastUsed lastUsed timestamp to use
    * @param maxIdle maxIdle to use
    * @return a new InternalCacheEntry
    */
   //OffHeapInternalCacheEntry create(Object key, Object value, OffHeapMetadata metadata, long created, long lifespan, long lastUsed, long maxIdle);

   /**
    * Creates a new {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} instance
    * @param key key to use
    * @param value value to use
    * @param version version to use
    * @param created creation timestamp to use
    * @param lifespan lifespan to use
    * @param lastUsed lastUsed timestamp to use
    * @param maxIdle maxIdle to use
    * @return a new InternalCacheEntry
    */
   // To be deprecated, once metadata object can be retrieved remotely...
   OffHeapInternalCacheEntry create(Object key, Object value, OffHeapEntryVersion version, long created, long lifespan, long lastUsed, long maxIdle);

   /**
    * TODO: Adjust javadoc
    *
    * Updates an existing {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} with new metadata.  This may result in a new
    * {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry}
    * instance being created, as a different {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} implementation
    * may be more appropriate to suit the new metadata values.  As such, one should consider the
    * {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry}
    * passed in as a parameter as passed by value and not by reference.
    *
    * @param metadata new metadata
    * @return a new InternalCacheEntry instance
    */


    OffHeapInternalCacheEntry create(Object key, Object value, OffHeapMetadata metadata, long created, long lifespan, long lastUsed, long maxIdle);

    /**
    * Creates an {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheValue} based on the {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry} passed in.
    *
    * @param cacheEntry to use to generate a {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheValue}
    * @return an {@link org.infinispan.offheap.container.entries.OffHeapInternalCacheValue}
    */
   OffHeapInternalCacheValue createValue(OffHeapCacheEntry cacheEntry);

    // TODO: Do we need this???
    OffHeapInternalCacheEntry create(Object key, Object value, OffHeapMetadata metadata, long lifespan, long maxIdle);

    OffHeapInternalCacheEntry update(OffHeapInternalCacheEntry ice, OffHeapMetadata metadata);
}
