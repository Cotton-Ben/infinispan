package org.infinispan.offheap.container.entries;

/**
 * An entry that may have state, such as created, changed, valid, etc.
 *
 * @author Manik Surtani
 * @since 5.1
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public interface OffHeapStateChangingEntry {

   byte getStateFlags();

   void copyStateFlagsFrom(OffHeapStateChangingEntry other);

}
