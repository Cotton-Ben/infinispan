package org.infinispan.offheap.container;

import net.jcip.annotations.ThreadSafe;
import net.openhft.collections.SharedHashMapBuilder;
import org.infinispan.commons.equivalence.Equivalence;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.util.CollectionFactory;
import org.infinispan.commons.util.concurrent.ParallelIterableMap;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.eviction.*;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.offheap.BondVOInterface;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.offheap.util.concurrent.OffHeapParallelIterableMap;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.offheap.util.OffHeapCoreImmutables;
import org.infinispan.util.CoreImmutables;
import org.infinispan.util.TimeService;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap.Eviction;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap.EvictionListener;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * OffHeap OffHeapDefaultDataContainer is both eviction and non-eviction based data container.
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
@ThreadSafe
public class OffHeapDefaultDataContainer implements OffHeapDataContainer {

   private static final Log log = LogFactory.getLog(OffHeapDefaultDataContainer.class);
   private static final boolean trace = log.isTraceEnabled();

   protected ConcurrentMap<Object, OffHeapInternalCacheEntry> entries;
   protected OffHeapInternalEntryFactory entryFactory;
   protected DefaultEvictionListener evictionListener;
   private EvictionManager evictionManager;
   private PassivationManager passivator;
   private ActivationManager activator;
   private PersistenceManager pm;
   private TimeService timeService;

   public OffHeapDefaultDataContainer(
           Class<String> stringClass,
           Class<BondVOInterface> bondVOInterfaceClass,
           String bondVOOperand,
           int entrysSize,
           int segmentsSize) {
       try {
           long t = System.currentTimeMillis();
           ConcurrentMap<String, BondVOInterface> entries = new SharedHashMapBuilder()
                   .generatedValueType(Boolean.TRUE)
                   .entrySize(entrysSize)
                   .minSegments(segmentsSize)
                   .create(
                           new File("/dev/shm/" + bondVOOperand + ".@t=" + t),
                           String.class,
                           BondVOInterface.class
                   );
           System.out.println("OpenHFT /dev/shmSHM/bondVO.@t="+t+"  entries=["+
                   (
                   (entries!=null) ? entries.toString() : "NULL"
                    ) +
                   "]");
       } catch (Exception e) {
           e.printStackTrace();
       }

        //entries = CollectionFactory.makeConcurrentParallelMap(128, concurrencyLevel);
        evictionListener = null;
      }





//    public OffHeapDefaultDataContainer(int concurrencyLevel) {
//    }
//
//   @Inject
//   public void initialize(
//                EvictionManager evictionManager,
//                PassivationManager passivator,
//                OffHeapInternalEntryFactory entryFactory,
//                ActivationManager activator,
//                PersistenceManager clm,
//                TimeService timeService) {
//      this.evictionManager = evictionManager;
//      this.passivator = passivator;
//      this.entryFactory = entryFactory;
//      this.activator = activator;
//      this.pm = clm;
//      this.timeService = timeService;
//   }

//   public static OffHeapDataContainer boundedDataContainer(int concurrencyLevel, int maxEntries,
//            EvictionStrategy strategy, EvictionThreadPolicy policy,
//            Equivalence keyEquivalence, Equivalence valueEquivalence) {
//      return new OffHeapDefaultDataContainer(concurrencyLevel, maxEntries, strategy,
//            policy, keyEquivalence, valueEquivalence);
//   }
//
//   public static OffHeapDataContainer unBoundedDataContainer(int concurrencyLevel,
//         Equivalence keyEquivalence, Equivalence valueEquivalence) {
//      return new OffHeapDefaultDataContainer(concurrencyLevel, keyEquivalence, valueEquivalence);
//   }

//   public static OffHeapDataContainer unBoundedDataContainer(int concurrencyLevel) {
//      return new OffHeapDefaultDataContainer(concurrencyLevel);
//   }

   //@Override
   public OffHeapInternalCacheEntry peek(Object key) {
      return entries.get(key);
   }



    @Override
   public OffHeapInternalCacheEntry get(Object k) {
      OffHeapInternalCacheEntry e = peek(k);
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
   public void put(Object k, Object v, OffHeapMetadata metadata) {
      OffHeapInternalCacheEntry e = entries.get(k);
      if (e != null) {
         e.setValue(v);
         OffHeapInternalCacheEntry original = e;
         e = entryFactory.update(e, metadata);
         // we have the same instance. So we need to reincarnate, if mortal.
         if (isMortalEntry(e) && original == e) {
            e.reincarnate(timeService.wallClockTime());
         }
      } else {
         // this is a brand-new entry
         e = entryFactory.create(k, v,  metadata);
      }

      if (trace)
         log.tracef("Store %s in container", e);

      entries.put(k, e);
   }

   private boolean isMortalEntry(OffHeapInternalCacheEntry e) {
      return e.getLifespan() > 0;
   }

   @Override
   public boolean containsKey(Object k) {
      OffHeapInternalCacheEntry ice = peek(k);
      if (ice != null && ice.canExpire() && ice.isExpired(timeService.wallClockTime())) {
         entries.remove(k);
         ice = null;
      }
      return ice != null;
   }

   @Override
   public OffHeapInternalCacheEntry remove(Object k) {
      OffHeapInternalCacheEntry e = entries.remove(k);
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
      return Collections.unmodifiableSet(entries.keySet());
   }

   @Override
   public Collection<Object> values() {
      return new Values();
   }

   @Override
   public Set<OffHeapInternalCacheEntry> entrySet() {
      return new EntrySet();
   }

   @Override
   public void purgeExpired() {
      long currentTimeMillis = timeService.wallClockTime();
      for (Iterator<OffHeapInternalCacheEntry> purgeCandidates = entries.values().iterator(); purgeCandidates.hasNext();) {
         OffHeapInternalCacheEntry e = purgeCandidates.next();
         if (e.isExpired(currentTimeMillis)) {
            purgeCandidates.remove();
         }
      }
   }



   @Override
   public Iterator iterator() {

      return new EntryIterator( entries.values().iterator() );
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
      ImmutableEntryIterator(Iterator<OffHeapInternalCacheEntry> it){
         super(it);
      }

      @Override
      public OffHeapInternalCacheEntry next() {
         return OffHeapCoreImmutables.immutableInternalCacheEntry(super.next());
   }
   }

   public static class EntryIterator implements Iterator<OffHeapInternalCacheEntry> {

      private final Iterator<OffHeapInternalCacheEntry> it;

      EntryIterator(Iterator<OffHeapInternalCacheEntry> it){this.it=it;}

      @Override
      public OffHeapInternalCacheEntry next() {
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
   private class EntrySet extends AbstractSet<OffHeapInternalCacheEntry> {

      @Override
      public boolean contains(Object o) {
         if (!(o instanceof Map.Entry)) {
            return false;
         }

         @SuppressWarnings("rawtypes")
         Map.Entry e = (Map.Entry) o;
         OffHeapInternalCacheEntry ice = entries.get(e.getKey());
         if (ice == null) {
            return false;
         }
         return ice.getValue().equals(e.getValue());
      }

      @Override
      public Iterator<OffHeapInternalCacheEntry> iterator() {
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
      Iterator<OffHeapInternalCacheEntry> currentIterator;

      private ValueIterator(Iterator<OffHeapInternalCacheEntry> it) {
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
           final ParallelIterableMap.KeyValueAction<Object, OffHeapInternalCacheEntry> action
   ) throws InterruptedException{
      if (filter == null)
         throw new IllegalArgumentException("No filter specified");
      if (action == null)
         throw new IllegalArgumentException("No action specified");

      OffHeapParallelIterableMap<Object, OffHeapInternalCacheEntry> map =
              (OffHeapParallelIterableMap<Object, OffHeapInternalCacheEntry>) entries;

       map.forEach(512, new OffHeapParallelIterableMap.KeyValueAction<Object, OffHeapInternalCacheEntry>() {

           @Override
           public void apply(Object o, OffHeapInternalCacheEntry internalCacheEntry) {

           }


//         @Override
//         public void apply(Object key, OffHeapInternalCacheEntry value) {
//            if (filter.shouldLoadKey((K)key)) {
//               action.apply((K)key, value);
//            }
//         }
      });
      //TODO figure out the way how to do interruption better (during iteration)
      if(Thread.currentThread().isInterrupted()){
         throw new InterruptedException();
      }
   }

    @Override
    public void initialize(Object o, Object o1, OffHeapInternalEntryFactoryImpl internalEntryFactory, Object o2, Object o3, TimeService timeService) {

    }
}
