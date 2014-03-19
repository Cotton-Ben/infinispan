package org.infinispan.offheap.container.versioning;

import org.infinispan.Cache;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;

/**
 * A version generator implementation for SimpleClusteredVersions
 *
 * @author Manik Surtani
 * @since 5.1
 */
public class OffHeapSimpleClusteredVersionGenerator implements OffHeapVersionGenerator {
   // The current cache topology ID is recorded and used as a part of the version generated, and as such used as the
   // most significant part of a version comparison. If a version is generated based on an old cache topology and another is
   // generated based on a newer topology, the one based on the newer topology wins regardless of the version's counter.
   // See SimpleClusteredVersion for more details.
   private volatile int topologyId = -1;

   private static final OffHeapSimpleClusteredVersion NON_EXISTING = new OffHeapSimpleClusteredVersion(0, 0);

   private Cache<?, ?> cache;

   @Inject
   public void init(Cache<?, ?> cache) {
      this.cache = cache;
   }

   @Start(priority = 11)
   public void start() {
      cache.addListener(new TopologyIdUpdater());
   }

   @Override
   public OffHeapIncrementableEntryVersion generateNew() {
      if (topologyId == -1) {
         throw new IllegalStateException("Topology id not set yet");
      }
      return new OffHeapSimpleClusteredVersion(topologyId, 1);
   }

   @Override
   public OffHeapIncrementableEntryVersion increment(OffHeapIncrementableEntryVersion initialVersion) {
      if (initialVersion instanceof OffHeapSimpleClusteredVersion) {
         OffHeapSimpleClusteredVersion old = (OffHeapSimpleClusteredVersion) initialVersion;
         return new OffHeapSimpleClusteredVersion(topologyId, old.version + 1);
      } else {
         throw new IllegalArgumentException("I only know how to deal with SimpleClusteredVersions, not " + initialVersion.getClass().getName());
      }
   }

   @Override
   public OffHeapIncrementableEntryVersion nonExistingVersion() {
      return NON_EXISTING;
   }

   @Listener
   public class TopologyIdUpdater {

      @TopologyChanged
      public void onTopologyChange(TopologyChangedEvent<?, ?> tce) {
         topologyId = tce.getNewTopologyId();
      }
   }
}
