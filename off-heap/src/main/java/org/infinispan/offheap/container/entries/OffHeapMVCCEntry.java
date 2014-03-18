package org.infinispan.offheap.container.entries;

import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.metadata.OffHeapMetadata;

/**
 * An entry that can be safely copied when updates are made, to provide MVCC semantics
 *
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public interface OffHeapMVCCEntry extends OffHeapCacheEntry, OffHeapStateChangingEntry {

    long getLifespan();

    long getMaxIdle();

    Object getKey();

    Object getValue();

    Object setValue(Object value);

    boolean isNull();

    void copyForUpdate(OffHeapDataContainer container);

    void commit(OffHeapDataContainer container, OffHeapMetadata providedMetadata);

    void rollback();

    boolean isChanged();

    void setChanged(boolean isChanged);

    void setSkipLookup(boolean skipLookup);

    boolean skipLookup();

    boolean isValid();

    void setValid(boolean valid);

    OffHeapMetadata getMetadata();

    void setMetadata(OffHeapMetadata metadata);

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
