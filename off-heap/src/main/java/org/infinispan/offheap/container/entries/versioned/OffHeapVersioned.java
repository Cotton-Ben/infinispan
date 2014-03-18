package org.infinispan.offheap.container.entries.versioned;

import org.infinispan.offheap.container.versioning.OffHeapEntryVersion;

/**
 * An interface that marks the ability to handle versions
 *
 * @author Manik Surtani
 * @since 5.1
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public interface OffHeapVersioned {

   /**
    * @return the version of the entry.  May be null if versioning is not supported, and must never be null if
    *         versioning is supported.
    */
   OffHeapEntryVersion getVersion();

   /**
    * Sets the version on this entry.
    *
    * @param version version to set
    */
   void setVersion(OffHeapEntryVersion version);
}
