package org.infinispan.offheap.metadata;

/**
 * @author Mircea Markus
 * @since 6.0
 */
public interface OffHeapInternalMetadata extends OffHeapMetadata {

   long created();

   long lastUsed();

   boolean isExpired(long now);

   long expiryTime();
}
