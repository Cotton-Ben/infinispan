package org.infinispan.offheap.container.entries;

import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.DataContainer;
import org.infinispan.offheap.container.OffHeapDataContainer;

/**
 * An entry that can be safely copied when updates are made, to provide MVCC semantics
 *
 * @author Manik Surtani
 * @since 4.0
 */
public interface MVCCEntry extends OffHeapCacheEntry, StateChangingEntry {

    long getLifespan();

    long getMaxIdle();

    /**
    * Makes internal copies of the entry for updates
    *
    * @param container      data container
    */
   void copyForUpdate(OffHeapDataContainer container);

    Object getKey();

    Object getValue();

    Object setValue(Object value);

    boolean isNull();

    void copyForUpdate(OffHeapDataContainer container);

    void commit(OffHeapDataContainer container, Metadata providedMetadata);

    void rollback();

    boolean isChanged();

    void setChanged(boolean isChanged);

    void setSkipLookup(boolean skipLookup);

    boolean skipLookup();

    boolean isValid();

    void setValid(boolean valid);

    Metadata getMetadata();

    void setMetadata(Metadata metadata);

    boolean isCreated();

    void setCreated(boolean created);

    boolean isRemoved();

    boolean isEvicted();

    void setRemoved(boolean removed);

    void setEvicted(boolean evicted);

    boolean isLoaded();

    void setLoaded(boolean loaded);

    boolean undelete(boolean doUndelete);
}
