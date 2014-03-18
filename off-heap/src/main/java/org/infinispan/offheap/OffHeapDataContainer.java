package org.infinispan.offheap;

import net.openhft.collections.SharedHashMapBuilder;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.container.DataContainer;
import org.infinispan.container.DefaultDataContainer;
import org.infinispan.container.InternalEntryFactory;
import org.infinispan.metadata.Metadata;
import org.infinispan.commons.equivalence.Equivalence;
import org.infinispan.commons.util.concurrent.ParallelIterableMap;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.eviction.ActivationManager;
import org.infinispan.eviction.EvictionManager;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionThreadPolicy;
import org.infinispan.eviction.PassivationManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.util.CoreImmutables;
import org.infinispan.util.TimeService;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap.EvictionListener;

import java.io.File;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

//import static org.infinispan.commons.util.CollectionFactory.makeConcurrentParallelMap;

/**
 * OffHeapDefaultDataContainer is both eviction and non-eviction based data container.
 *
 *
 *
 *
 * @author <a href="https://github.com/dgor/">Dmitry.Gordeev@jpmorgan.com</a>
 * @author <a href="https://github.com/Cotton-Ben/">Ben.Cotton@jpmorgan.com</a>
 * @author <a href="https://github.com/OpenHFT/">Peter.Lawrey@higherfrequencytrading.com</a>
 *
 *
 * @since 4.0
 */
//@ThreadSafe
public class OffHeapDataContainer implements org.infinispan.container.DataContainer {

    private static final Log log = LogFactory.getLog(DefaultDataContainer.class);
    private static final boolean trace = log.isTraceEnabled();

    protected ConcurrentMap entries;
    protected InternalEntryFactory entryFactory;
    final protected DefaultEvictionListener evictionListener;
    private EvictionManager evictionManager;
    private PassivationManager passivator;
    private ActivationManager activator;
    private PersistenceManager pm;
    private TimeService timeService;

    public OffHeapDataContainer(
                                Class<String> keyClazz,
                                Class<BondVOInterface> valueClazz,
                                String operandFileName,
                                int entrySize,
                                int segSize
                                )    {
        entries = null;
        try {
            entries = new SharedHashMapBuilder()
                    .generatedValueType(Boolean.TRUE)
                    .entrySize(entrySize)
                    .minSegments(segSize)
                    .create(
                            new File("/dev/shm/" + operandFileName),
                            keyClazz,
                            valueClazz
                    );
        } catch (IOException e) {
            e.printStackTrace();
        }
        evictionListener = null;
    }




    @Inject
    public void initialize(EvictionManager evictionManager,
                           PassivationManager passivator,
                           InternalEntryFactory entryFactory,
                           ActivationManager activator,
                           PersistenceManager clm,
                           TimeService timeService) {
        this.evictionManager = evictionManager;
        this.passivator = passivator;
        this.entryFactory = entryFactory;
        this.activator = activator;
        this.pm = clm;
        this.timeService = timeService;
    }

    public static DataContainer boundedDataContainer(int concurrencyLevel,
                                                     int maxEntries,
                                                     EvictionStrategy strategy,
                                                     EvictionThreadPolicy policy,
                                                     Equivalence keyEquivalence,
                                                     Equivalence valueEquivalence) {
        return new DefaultDataContainer(concurrencyLevel,
                maxEntries, strategy,
                policy,
                keyEquivalence,
                valueEquivalence);
    }

    public static DataContainer unBoundedDataContainer(int concurrencyLevel,
                                                       Equivalence keyEquivalence,
                                                       Equivalence valueEquivalence) {
        return new DefaultDataContainer(concurrencyLevel, keyEquivalence, valueEquivalence);
    }

    public static DataContainer unBoundedDataContainer(int concurrencyLevel) {
        return new DefaultDataContainer(concurrencyLevel);
    }

    @Override
    public InternalCacheEntry peek(Object key) {
        return (InternalCacheEntry) entries.get(key);
    }

    @Override
    public InternalCacheEntry get(Object k) {
        InternalCacheEntry e = peek(k);
        if (e != null && e.canExpire()) {
            long currentTimeMillis = timeService.wallClockTime();
            if (e.isExpired(currentTimeMillis)) {
                entries.remove(k);
                e = null;
            } else {
                e.touch(currentTimeMillis);
            }
        }
        return e;
    }

    @Override
    public void put(Object k, Object v, Metadata metadata) {
       InternalCacheEntry e = (InternalCacheEntry) entries.get(k);
        if (e != null) {
            e.setValue(v);
            Object original = e;
            e = entryFactory.update((InternalCacheEntry) e, metadata);
            // we have the same instance. So we need to reincarnate, if mortal.
            if (isMortalEntry(e) && original == e) {
                e.reincarnate(timeService.wallClockTime());
            }
        } else {
            // this is a brand-new entry
            e = entryFactory.create(k, v, metadata);
        }

        if (trace)
            log.tracef("Store %s in container", e);

        entries.put(String.valueOf(k),  e);
    }

    private boolean isMortalEntry(InternalCacheEntry e) {
        return e.getLifespan() > 0;
    }

    @Override
    public boolean containsKey(Object k) {
        InternalCacheEntry ice = peek(k);
        if (ice != null && ice.canExpire() && ice.isExpired(timeService.wallClockTime())) {
            entries.remove(k);
            ice = null;
        }
        return ice != null;
    }

    @Override
    public InternalCacheEntry remove(Object k) {
        InternalCacheEntry e = (InternalCacheEntry) entries.remove(k);
        return e == null || (e.canExpire() && e.isExpired(timeService.wallClockTime())) ? null : e;
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public Set<Object> keySet() {
        return null;
        //return entries.keySet();
    }

//    //@Override
//    public Set<Object> keySet() {
//        return Collections.unmodifiableSet(entries.keySet());
//    }

    @Override
    public Collection<Object> values() {
        return new Values();
    }

    @Override
    public Set<InternalCacheEntry> entrySet() {
        return new EntrySet();
    }

    @Override
    public void purgeExpired() {
        long currentTimeMillis = timeService.wallClockTime();
        for (Iterator<BondVOInterface> purgeCandidates = entries.values().iterator(); purgeCandidates.hasNext();) {
            InternalCacheEntry e = purgeCandidates.next();
            if (e.isExpired(currentTimeMillis)) {
                purgeCandidates.remove();
            }
        }
    }

    @Override
    public Iterator<InternalCacheEntry> iterator() {
        return new EntryIterator(entries.values().iterator());
    }

    private final class DefaultEvictionListener implements EvictionListener<Object, InternalCacheEntry> {

        @Override
        public void onEntryEviction(Map<Object, InternalCacheEntry> evicted) {
            evictionManager.onEntryEviction(evicted);
        }

        @Override
        public void onEntryChosenForEviction(InternalCacheEntry entry) {
            passivator.passivate(entry);
        }

        @Override
        public void onEntryActivated(Object key) {
            activator.activate(key);
        }

        @Override
        public void onEntryRemoved(Object key) {
            if (pm != null)
                pm.deleteFromAllStores(key, false);
        }
    }

    private static class ImmutableEntryIterator extends EntryIterator {
        ImmutableEntryIterator(Iterator<BondVOInterface> it){
            super(it);
        }

        @Override
        public InternalCacheEntry next() {
            return CoreImmutables.immutableInternalCacheEntry(super.next());
        }
    }

    public static class EntryIterator implements Iterator<InternalCacheEntry> {

        private final Iterator<BondVOInterface> it;

        EntryIterator(Iterator<BondVOInterface> it){this.it=it;}

        @Override
        public InternalCacheEntry next() {
            return it.next();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Minimal implementation needed for unmodifiable Set
     *
     */
    private class EntrySet extends AbstractSet<InternalCacheEntry> {

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            @SuppressWarnings("rawtypes")
            Map.Entry e = (Map.Entry) o;
            InternalCacheEntry ice = (InternalCacheEntry) entries.get(e.getKey());
            if (ice == null) {
                return false;
            }
            return ice.getValue().equals(e.getValue());
        }

        @Override
        public Iterator<InternalCacheEntry> iterator() {
            return new ImmutableEntryIterator(entries.values().iterator());
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public String toString() {
            return entries.toString();
        }
    }

    /**
     * Minimal implementation needed for unmodifiable Collection
     *
     */
    private class Values extends AbstractCollection<Object> {
        @Override
        public Iterator<Object> iterator() {
            return new ValueIterator(entries.values().iterator());
        }

        @Override
        public int size() {
            return entries.size();
        }
    }

    private static class ValueIterator implements Iterator<Object> {
        Iterator<BondVOInterface> currentIterator;

        private ValueIterator(Iterator<BondVOInterface> it) {
            currentIterator = it;
        }

        @Override
        public boolean hasNext() {
            return currentIterator.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object next() {
            return currentIterator.next().getValue();
        }
    }

    @Override
    public <K> void executeTask(
                                    final AdvancedCacheLoader.KeyFilter<K> filter,
                                    final ParallelIterableMap.KeyValueAction<Object, InternalCacheEntry> action
                                )   throws InterruptedException{
        if (filter == null)
            throw new IllegalArgumentException("No filter specified");
        if (action == null)
            throw new IllegalArgumentException("No action specified");

        ParallelIterableMap<Object, InternalCacheEntry> map = (ParallelIterableMap<Object, InternalCacheEntry>) entries;
        map.forEach(512, new ParallelIterableMap.KeyValueAction<Object, InternalCacheEntry>() {
            @Override
            public void apply(Object key, InternalCacheEntry value) {
                if (filter.shouldLoadKey((K)key)) {
                    action.apply((K)key, value);
                }
            }
        });
        //TODO figure out the way how to do interruption better (during iteration)
        if(Thread.currentThread().isInterrupted()){
            throw new InterruptedException();
        }
    }
}
