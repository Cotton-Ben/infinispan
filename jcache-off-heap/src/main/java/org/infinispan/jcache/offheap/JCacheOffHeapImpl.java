package org.infinispan.jcache.offheap;

import net.openhft.collections.SharedHashMapBuilder;
import net.openhft.collections.VanillaSharedHashMap;
import org.infinispan.commons.util.concurrent.ParallelIterableMap;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by
 *
 *@author dmitry.gordeev@jpmorgan.com
 *@author ben.cotton@jpmorgan.com
 *<p> </p>
 *@author Extended from OpenHFT HC solution (peter.lawrey@higherfrequencytrading.com)
 *<p></p>
 *@since 7.0
 *
 * on 3/8/2014
 *
 */

public class JCacheOffHeapImpl extends VanillaSharedHashMap implements DataContainer {

    public JCacheOffHeapImpl(SharedHashMapBuilder builder, File file, Class aClass, Class aClass2) throws IOException {
        super(builder, file, aClass, aClass2);
    }

    @Override
    public InternalCacheEntry get(Object k) {

        return (InternalCacheEntry) super.get(k);  //Dmitry: we have to morph to return RedHat 'ICE'
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

        return super.containsKey(k);
    }

    @Override
    public InternalCacheEntry remove(Object k) {
        return (InternalCacheEntry) super.remove(k);  //Dmitry: we have morph to return RedHat 'ICE'
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public void clear() {
        super.clear();
    }


    @Override
    public Set<Object> keySet() {
        return super.keySet();
    }

    @Override
    public Collection<Object> values() {
        return super.values();
    }

    @Override
    public Set<InternalCacheEntry> entrySet() {
        return super.entrySet();
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
