package org.infinispan.offheap.metadata;

/**
 * @author Mircea Markus
 * @since 6.0
 */
public interface InternalMetadata extends Metadata {

   long created();

   long lastUsed();

   boolean isExpired(long now);

   long expiryTime();
}
