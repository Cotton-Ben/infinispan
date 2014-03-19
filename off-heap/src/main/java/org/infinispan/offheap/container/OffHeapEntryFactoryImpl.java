package org.infinispan.offheap.container;

import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.container.EntryFactoryImpl;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.Metadatas;
import org.infinispan.offheap.notifications.cachelistener.OffHeapCacheNotifier;
import org.infinispan.offheap.container.entries.*;
import org.infinispan.offheap.context.OffHeapInvocationContext;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadatas;
import org.infinispan.offheap.notifications.cachelistener.OffHeapCacheNotifier;
import org.infinispan.util.concurrent.IsolationLevel;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * {@link OffHeapEntryFactory} implementation to be used for optimistic locking scheme.
 *
 * @author Mircea Markus
 * @since 5.1
 */
public class OffHeapEntryFactoryImpl implements OffHeapEntryFactory {

   private static final Log log = LogFactory.getLog(EntryFactoryImpl.class);
   private final boolean trace = log.isTraceEnabled();

   protected boolean useRepeatableRead;
   private OffHeapDataContainer container;
   protected boolean clusterModeWriteSkewCheck;
   private boolean isL1Enabled; //cache the value
   private Configuration configuration;
   private OffHeapCacheNotifier notifier;
   private DistributionManager distributionManager;//is null for non-clustered caches

   @Inject
   public void injectDependencies(
                                    OffHeapDataContainer dataContainer,
                                    Configuration configuration,
                                    OffHeapCacheNotifier notifier,
                                    DistributionManager distributionManager) {
      this.container = dataContainer;
      this.configuration = configuration;
      this.notifier = notifier;
      this.distributionManager = distributionManager;
   }

   @Start (priority = 8)
   public void init() {
      useRepeatableRead = configuration.locking().isolationLevel() == IsolationLevel.REPEATABLE_READ;
      clusterModeWriteSkewCheck = useRepeatableRead && configuration.locking().writeSkewCheck() &&
            configuration.clustering().cacheMode().isClustered() && configuration.versioning().scheme() == VersioningScheme.SIMPLE &&
            configuration.versioning().enabled();
      isL1Enabled = configuration.clustering().l1().enabled();
   }

    @Override
    public OffHeapCacheEntry wrapEntryForReading(InvocationContext ctx, Object key) throws InterruptedException {
        return null;
    }

    @Override
   public final OffHeapCacheEntry wrapEntryForReading(OffHeapInvocationContext ctx, Object key) throws InterruptedException {
      OffHeapCacheEntry cacheEntry = this.offHeapGetFromContext(ctx, key);
      if (cacheEntry == null) {
         cacheEntry = this.offHeapGetFromContainer(key, false);

         // do not bother wrapping though if this is not in a tx.  repeatable read etc are all meaningless unless there is a tx.
         if (useRepeatableRead) {
            OffHeapMVCCEntry mvccEntry;
            if (cacheEntry == null) {
               mvccEntry = createWrappedEntry(key, null, ctx, null, false, false, false);
            } else {
               mvccEntry = createWrappedEntry(key, cacheEntry, ctx, null, false, false, false);
               // If the original entry has changeable state, copy state flags to the new MVCC entry.
               if (cacheEntry instanceof OffHeapStateChangingEntry && mvccEntry != null)
                  mvccEntry.copyStateFlagsFrom((OffHeapStateChangingEntry) cacheEntry);
            }

            if (mvccEntry != null) ctx.offHeapPutLookedUpEntry(key, mvccEntry);
            if (trace) {
               log.tracef("Wrap %s for read. Entry=%s", key, mvccEntry);
            }
            return mvccEntry;
         } else if (cacheEntry != null) { // if not in transaction and repeatable read, or simply read committed (regardless of whether in TX or not), do not wrap
            ctx.offHeapPutLookedUpEntry(key, cacheEntry);
         }
         if (trace) {
            log.tracef("Wrap %s for read. Entry=%s", key, cacheEntry);
         }
         return cacheEntry;
      }
      if (trace) {
         log.tracef("Wrap %s for read. Entry=%s", key, cacheEntry);
      }
      return cacheEntry;
   }



    @Override
   public final OffHeapMVCCEntry wrapEntryForClear(InvocationContext ctx, Object key) throws InterruptedException {
      //skipRead == true because the keys values are not read during the ClearOperation (neither by application)
      OffHeapMVCCEntry mvccEntry = this.offHeapWrapEntry(ctx, key, null, true);
      if (trace) {
         log.tracef("Wrap %s for clear. Entry=%s", key, mvccEntry);
      }
      return mvccEntry;
   }

    private OffHeapMVCCEntry offHeapWrapEntry(InvocationContext ctx, Object key, Object o, boolean b) {
        return null;
    }


    private OffHeapCacheEntry offHeapGetFromContext(InvocationContext ctx, Object key) {
        return null;
    }
   @Override
   public final OffHeapMVCCEntry wrapEntryForReplace(InvocationContext ctx, ReplaceCommand cmd) throws InterruptedException {
      Object key = cmd.getKey();
      OffHeapMVCCEntry mvccEntry = this.offHeapWrapEntry(ctx, key, cmd.getMetadata(), false);
      if (mvccEntry == null) {
         // make sure we record this! Null value since this is a forced lock on the key
         ctx.putLookedUpEntry(key, null);
      }
      if (trace) {
         log.tracef("Wrap %s for replace. Entry=%s", key, mvccEntry);
      }
      return mvccEntry;
   }

   @Override
   public final OffHeapMVCCEntry wrapEntryForRemove(InvocationContext ctx, Object key, boolean skipRead,
                                              boolean forInvalidation, boolean forceWrap) throws InterruptedException {
      OffHeapCacheEntry cacheEntry = offHeapGetFromContext(ctx, key);
      OffHeapMVCCEntry mvccEntry = null;
      if (cacheEntry != null) {
         if (cacheEntry instanceof OffHeapMVCCEntry) {
            mvccEntry = (OffHeapMVCCEntry) cacheEntry;
         } else {
            //skipRead == true because the key already exists in the context that means the key was previous accessed.
            mvccEntry = offHeapWrapMvccEntryForRemove(ctx, key, cacheEntry, true);
         }
      } else {
         OffHeapInternalCacheEntry ice = offHeapFromContainer(key, forInvalidation);
         if (ice != null || clusterModeWriteSkewCheck || forceWrap) {
            mvccEntry = offHeapWrapInternalCacheEntryForPut(ctx, key, ice, null, skipRead);
         }
      }
      if (mvccEntry == null) {
         // make sure we record this! Null value since this is a forced lock on the key
         ctx.putLookedUpEntry(key, null);
      } else {
         mvccEntry.copyForUpdate(container);
      }
      if (trace) {
         log.tracef("Wrap %s for remove. Entry=%s", key, mvccEntry);
      }
      return mvccEntry;
   }

    private OffHeapMVCCEntry offHeapWrapMvccEntryForRemove(InvocationContext ctx, Object key, OffHeapCacheEntry cacheEntry, boolean skipRead) {
        return null;
    }

    private OffHeapMVCCEntry offHeapWrapInternalCacheEntryForPut(InvocationContext ctx, Object key, OffHeapInternalCacheEntry ice, Object o, boolean skipRead) {
        return null;
    }

    private OffHeapInternalCacheEntry offHeapFromContainer(Object key, boolean forInvalidation) {
       return null;
    }

    @Override
   //removed final modifier to allow mock this method
   public OffHeapMVCCEntry wrapEntryForPut(
            OffHeapInvocationContext ctx,
            Object key,
            OffHeapInternalCacheEntry icEntry,
            boolean undeleteIfNeeded,
            OffHeapFlagAffectedCommand cmd,
            boolean skipRead)                throws InterruptedException {
      OffHeapCacheEntry cacheEntry = offHeapGetFromContext(ctx, key);
      OffHeapMVCCEntry mvccEntry;
      if (cacheEntry != null && cacheEntry.isNull() && !useRepeatableRead) cacheEntry = null;
      OffHeapMetadata providedMetadata = cmd.GetOffHeapMetadata();
      if (cacheEntry != null) {
         if (useRepeatableRead) {
            //sanity check. In repeatable read, we only deal with RepeatableReadEntry and ClusteredRepeatableReadEntry
            if (cacheEntry instanceof OffHeapRepeatableReadEntry) {
               mvccEntry = (OffHeapMVCCEntry) cacheEntry;
            } else {
               throw new IllegalStateException("Cache entry stored in context should be a RepeatableReadEntry instance " +
                                                     "but it is " + cacheEntry.getClass().getCanonicalName());
            }
            //if the icEntry is not null, then this is a remote get. We need to update the value and the metadata.
            if (!mvccEntry.isRemoved() && !mvccEntry.skipLookup() && icEntry != null) {
               mvccEntry.setValue(icEntry.getValue());
               this.offHeapUpdateVersion(mvccEntry, icEntry.getMetadata());
            }
            if (!mvccEntry.isRemoved() && mvccEntry.isNull()) {
               //new entry
               mvccEntry.setCreated(true);
            }
            //always update the metadata if needed.
            this.offHeapUpdateMetadata(mvccEntry, providedMetadata);

         } else {
            //skipRead == true because the key already exists in the context that means the key was previous accessed.
            mvccEntry = offHeapWrapMvccEntryForPut(ctx, key, cacheEntry, providedMetadata, true);
         }
         mvccEntry.undelete(undeleteIfNeeded);
      } else {
         OffHeapInternalCacheEntry ice = (icEntry == null ? offHeapGetFromContainer(key, false) : icEntry);
         // A putForExternalRead is putIfAbsent, so if key present, do nothing
         if (ice != null && cmd.hasFlag(Flag.PUT_FOR_EXTERNAL_READ)) {
            // make sure we record this! Null value since this is a forced lock on the key
            ctx.putLookedUpEntry(key, null);
            if (trace) {
               log.tracef("Wrap %s for put. Entry=null", key);
            }
            return null;
         }

         mvccEntry = ice != null ?
             this.offHeapWrapInternalCacheEntryForPut(ctx, key, ice, providedMetadata, skipRead) :
             this.offHeapNewMvccEntryForPut(ctx, key, cmd, providedMetadata, skipRead);
      }
      mvccEntry.copyForUpdate(container);
      if (trace) {
         log.tracef("Wrap %s for put. Entry=%s", key, mvccEntry);
      }
      return mvccEntry;
   }

    @Override
    public OffHeapCacheEntry wrapEntryForDelta(InvocationContext ctx, Object deltaKey, Delta delta) throws InterruptedException {
        return null;
    }

    @Override
    public OffHeapMVCCEntry wrapEntryForPut(InvocationContext ctx, Object key, OffHeapInternalCacheEntry ice, boolean undeleteIfNeeded, FlagAffectedCommand cmd, boolean skipRead) throws InterruptedException {
        return null;
    }

    private OffHeapMVCCEntry offHeapNewMvccEntryForPut(OffHeapInvocationContext ctx, Object key, OffHeapFlagAffectedCommand cmd, OffHeapMetadata providedMetadata, boolean skipRead) {
        return null;
    }

    private void offHeapUpdateMetadata(OffHeapMVCCEntry mvccEntry, OffHeapMetadata providedMetadata) {

    }

    private OffHeapMVCCEntry offHeapNewMvccEntryForPut(
                                                        OffHeapInvocationContext ctx,
                                                        Object key, FlagAffectedCommand cmd,
                                                        OffHeapMetadata providedMetadata,
                                                        boolean skipRead) {
        return null;
    }


    private void offHeapUpdateVersion(OffHeapMVCCEntry mvccEntry, OffHeapMetadata metadata) {
    }

   @Override
   public OffHeapCacheEntry wrapEntryForDelta(
           OffHeapInvocationContext ctx,
           Object deltaKey,
           Delta delta)                           throws InterruptedException {

        OffHeapCacheEntry cacheEntry = offHeapGetFromContext(ctx, deltaKey);
      OffHeapDeltaAwareCacheEntry deltaAwareEntry = null;
      if (cacheEntry != null) {        
//         deltaAwareEntry =  offHeapWrapInternalCacheEntryForPut(
//                  OffHeapInvocationContext,
//                  Object ,
//                  OffHeapInternalCacheEntry ,
//                  OffHeapMetadata ,
//                  boolean ) {} tryForDelta(ctx, deltaKey, cacheEntry);
      } else {                     
         OffHeapInternalCacheEntry ice = this.offHeapFromContainer(deltaKey, false);
         if (ice != null) {
            deltaAwareEntry = newDeltaAwareCacheEntry(ctx, deltaKey, (DeltaAware)ice.getValue());
         }
      }
      if (deltaAwareEntry != null)
         deltaAwareEntry.appendDelta(delta);
      if (trace) {
         log.tracef("Wrap %s for delta. Entry=%s", deltaKey, deltaAwareEntry);
      }
      return deltaAwareEntry;
   }
   
   private OffHeapDeltaAwareCacheEntry wrapEntryForDelta(OffHeapInvocationContext ctx, Object key, OffHeapCacheEntry cacheEntry) {
      if (cacheEntry instanceof OffHeapDeltaAwareCacheEntry) return (OffHeapDeltaAwareCacheEntry) cacheEntry;
      return offHeapWrapInternalCacheEntryForDelta(ctx, key, cacheEntry);
   }

    private OffHeapDeltaAwareCacheEntry offHeapWrapInternalCacheEntryForDelta(
                                                                        OffHeapInvocationContext ctx,
                                                                        Object key,
                                                                        OffHeapCacheEntry cacheEntry) {
        return null;
    }

    private OffHeapDeltaAwareCacheEntry wrapInternalCacheEntryForDelta(
                                                    OffHeapInvocationContext ctx,
                                                    Object key,
                                                    OffHeapCacheEntry cacheEntry) {
      OffHeapDeltaAwareCacheEntry e;
      if(cacheEntry instanceof OffHeapMVCCEntry){
         e = createWrappedDeltaEntry(key, (DeltaAware) cacheEntry.getValue(), cacheEntry);
      }
      else if (cacheEntry instanceof OffHeapInternalCacheEntry) {
         cacheEntry = offHeapWrapInternalCacheEntryForPut(ctx, key, (OffHeapInternalCacheEntry) cacheEntry, null, false);
         e = createWrappedDeltaEntry(key, (DeltaAware) cacheEntry.getValue(), cacheEntry);
      }
      else {
         e = createWrappedDeltaEntry(key, (DeltaAware) cacheEntry.getValue(), null);
      }
      ctx.offHeapPutLookedUpEntry(key, e);
      return e;

   }

    private OffHeapCacheEntry offHeapWrapInternalCacheEntryForPut(
                                                    OffHeapInvocationContext ctx,
                                                    Object key,
                                                    OffHeapInternalCacheEntry cacheEntry,
                                                    Object o,
                                                    boolean b) {
        return null;
    }

    private OffHeapCacheEntry offHeapGetFromContext(OffHeapInvocationContext ctx, Object key) {
      final OffHeapCacheEntry cacheEntry = ctx.lookupEntry(key);
      if (trace) log.tracef("Exists in context? %s ", cacheEntry);
      return cacheEntry;
   }

   private OffHeapInternalCacheEntry offHeapGetFromContainer(Object key, boolean forceFetch) {
      final boolean isLocal = distributionManager == null || distributionManager.getLocality(key).isLocal();
      final OffHeapInternalCacheEntry ice = isL1Enabled || isLocal || forceFetch ? container.get(key) : null;
      if (trace) log.tracef("Retrieved from container %s (isL1Enabled=%s, isLocal=%s)", ice, isL1Enabled, isLocal);
      return ice;
   }

   private OffHeapMVCCEntry newMvccEntryForPut(
                                    OffHeapInvocationContext ctx,
                                    Object key,
                                    FlagAffectedCommand cmd,
                                    OffHeapMetadata providedMetadata,
                                    boolean skipRead) {
      OffHeapMVCCEntry mvccEntry;
      if (trace) log.trace("Creating new entry.");
      Object v=null;
      boolean tf = true;
      this.notifier.offHeapNotifyCacheEntryCreated(key, v, tf, ctx, cmd);
      mvccEntry = this.offHeapCreateWrappedEntry(key, null, ctx, providedMetadata, true, false, skipRead);
      mvccEntry.setCreated(true);
      ctx.offHeapPutLookedUpEntry(key, mvccEntry);
      return mvccEntry;
   }
//    public  void offHeapNotifyCacheEntryCreated(
//                                                    Object key,
//                                                    Object v,
//                                                    boolean tf,
//                                                    OffHeapInvocationContext ctx,
//                                                    FlagAffectedCommand cmd) {
//
//    }
    private OffHeapMVCCEntry offHeapCreateWrappedEntry(
                                                    Object key,
                                                    Object o,
                                                    OffHeapInvocationContext ctx,
                                                    OffHeapMetadata providedMetadata,
                                                    boolean b,
                                                    boolean b1,
                                                    boolean skipRead) {
        return null;
    }

    private OffHeapMVCCEntry offHeapWrapMvccEntryForPut(
                                                    OffHeapInvocationContext ctx,
                                                    Object key,
                                                    OffHeapCacheEntry cacheEntry,
                                                    OffHeapMetadata providedMetadata,
                                                    boolean skipRead) {
      if (cacheEntry instanceof OffHeapMVCCEntry) {
         OffHeapMVCCEntry mvccEntry = (OffHeapMVCCEntry) cacheEntry;
         this.offHeapUpdateMetadata(mvccEntry, providedMetadata);
         return mvccEntry;
      }
      return offHeapWrapInternalCacheEntryForPut(
              ctx,
              key,
              (OffHeapInternalCacheEntry) cacheEntry,
              providedMetadata,
              skipRead);
   }

   private OffHeapMVCCEntry offHeapWrapInternalCacheEntryForPut(
                                                        OffHeapInvocationContext ctx,
                                                        Object key,
                                                        OffHeapInternalCacheEntry cacheEntry,
                                                        OffHeapMetadata providedMetadata,
                                                        boolean skipRead) {
      OffHeapMVCCEntry mvccEntry = offHeapCreateWrappedEntry(key, cacheEntry, ctx, providedMetadata, true, false, skipRead);
      ctx.offHeapPutLookedUpEntry(key, mvccEntry);
      return mvccEntry;
   }

   private OffHeapMVCCEntry offHeapWrapMvccEntryForRemove(OffHeapInvocationContext ctx,
                                                          Object key,
                                                          OffHeapCacheEntry cacheEntry,
                                                          boolean skipRead) {
      OffHeapMVCCEntry mvccEntry = offHeapCreateWrappedEntry(key, cacheEntry, ctx, null, false, true, skipRead);
      // If the original entry has changeable state, copy state flags to the new MVCC entry.
      if (cacheEntry instanceof OffHeapStateChangingEntry)
         mvccEntry.copyStateFlagsFrom((OffHeapStateChangingEntry) cacheEntry);

      ctx.offHeapPutLookedUpEntry(key, mvccEntry);
      return mvccEntry;
   }

   private OffHeapMVCCEntry wrapEntry(OffHeapInvocationContext ctx, Object key, OffHeapMetadata providedMetadata, boolean skipRead) {
      OffHeapCacheEntry cacheEntry = offHeapGetFromContext(ctx, key);
      OffHeapMVCCEntry mvccEntry = null;
      if (cacheEntry != null) {
         //already wrapped. set skip read to true to avoid replace the current version.
         mvccEntry = offHeapWrapMvccEntryForPut(ctx, key, cacheEntry, providedMetadata, true);
      } else {
         OffHeapInternalCacheEntry ice = offHeapGetFromContainer(key, false);
         if (ice != null || clusterModeWriteSkewCheck) {
            mvccEntry = this.offHeapwWrapInternalCacheEntryForPut(ctx, key, ice, providedMetadata, skipRead);
         }
      }
      if (mvccEntry != null)
         mvccEntry.copyForUpdate(container);
      return mvccEntry;
   }

    private OffHeapMVCCEntry offHeapwWrapInternalCacheEntryForPut(OffHeapInvocationContext ctx, Object key, OffHeapInternalCacheEntry ice, OffHeapMetadata providedMetadata, boolean skipRead) {
        return null;
    }

    protected OffHeapMVCCEntry createWrappedEntry(
                                            Object key,
                                            OffHeapCacheEntry cacheEntry,
                                            OffHeapInvocationContext context,
                                            OffHeapMetadata providedMetadata,
                                            boolean isForInsert,
                                            boolean forRemoval,
                                            boolean skipRead) {
      Object value = cacheEntry != null ? cacheEntry.getValue() : null;
      OffHeapMetadata metadata = providedMetadata != null
            ? providedMetadata
            : cacheEntry != null ? cacheEntry.getMetadata() : null;

      if (value == null && !isForInsert && !useRepeatableRead)
         return null;

      return useRepeatableRead
            ? new OffHeapClusteredRepeatableReadEntry(key, value, metadata)
            : new OffHeapClusteredRepeatableReadEntry(key, value, metadata); //yes, we know its a placeholder.
   }
   
   private OffHeapDeltaAwareCacheEntry newDeltaAwareCacheEntry(OffHeapInvocationContext ctx, Object key, DeltaAware deltaAware){
      OffHeapDeltaAwareCacheEntry deltaEntry = this.offHeapCreateWrappedDeltaEntry(key, deltaAware, null);
      ctx.offHeapPutLookedUpEntry(key, deltaEntry);
      return deltaEntry;
   }

    private OffHeapDeltaAwareCacheEntry offHeapCreateWrappedDeltaEntry(Object key, DeltaAware deltaAware, Object o) {
        return null;
    }

    private OffHeapDeltaAwareCacheEntry createWrappedDeltaEntry(Object key, DeltaAware deltaAware, OffHeapCacheEntry entry) {
      return new OffHeapDeltaAwareCacheEntry(key,deltaAware, entry);
   }

   private void updateMetadata(OffHeapMVCCEntry entry, OffHeapMetadata providedMetadata) {
      if (trace) {
         log.tracef("Update metadata for %s. Provided metadata is %s", entry, providedMetadata);
      }
      if (providedMetadata == null || entry == null || entry.getMetadata() != null) {
         return;
      }
      entry.setMetadata(providedMetadata);
   }

   private void updateVersion(OffHeapMVCCEntry entry, OffHeapMetadata providedMetadata) {
      if (trace) {
         log.tracef("Update metadata for %s. Provided metadata is %s", entry, providedMetadata);
      }
      if (providedMetadata == null || entry == null) {
         return;
      } else if (entry.getMetadata() == null) {
         entry.setMetadata(providedMetadata);
         return;
      }

      entry.setMetadata(OffHeapMetadatas.applyVersion(entry.getMetadata(), providedMetadata));
   }

}
