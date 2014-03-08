package org.infinispan.jcache.offheap;

import net.openhft.collections.SharedHashMapBuilder;
import net.openhft.collections.VanillaSharedHashMap;
import org.infinispan.commons.util.concurrent.ParallelIterableMap;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.spi.AdvancedCacheLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by
 *
 *@author ben.cotton@jpmorgan.com
 *@author dmitry.gordeev@jpmorgan.com
 *
 *@since 7.0
 *
 * on 3/8/14.
 *
 */

public class JCacheOffHeapImpl extends VanillaSharedHashMap implements DataContainer {

    public JCacheOffHeapImpl(SharedHashMapBuilder builder, File file, Class aClass, Class aClass2) throws IOException {
        super(builder, file, aClass, aClass2);
    }

    @Override
    public InternalCacheEntry get(Object k) {
        return null;
    }

    @Override
    public InternalCacheEntry peek(Object k) {
        return null;
    }

    @Override
    public void put(Object k, Object v, Metadata metadata) {

    }

    @Override
    public boolean containsKey(Object k) {
        return false;
    }

    @Override
    public InternalCacheEntry remove(Object k) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public Set<Object> keySet() {
        return null;
    }

    @Override
    public Collection<Object> values() {
        return null;
    }

    @Override
    public Set<InternalCacheEntry> entrySet() {
        return null;
    }

    @Override
    public void purgeExpired() {

    }

    @Override
    public <K> void executeTask(    AdvancedCacheLoader.KeyFilter<K> filter,
                                    ParallelIterableMap.KeyValueAction<Object,
                                    InternalCacheEntry> action
                                ) throws InterruptedException {

    }

    @Override
    public Iterator<InternalCacheEntry> iterator() {
        return null;
    }
}
