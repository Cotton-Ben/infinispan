package org.infinispan.offheap.container;

import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.container.DataContainer;
import org.infinispan.container.EntryFactory;
import org.infinispan.container.EntryFactoryImpl;
import org.infinispan.container.entries.*;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.Metadatas;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.util.concurrent.IsolationLevel;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 *
 * @author Mircea Markus
 * @since 5.1
 */
public class OffHeapEntryFactoryImpl implements EntryFactory {

   private static final Log log = LogFactory.getLog(EntryFactoryImpl.class);
   private final boolean trace = log.isTraceEnabled();

   protected boolean useRepeatableRead;
   private DataContainer container;
   protected boolean clusterModeWriteSkewCheck;
   private boolean isL1Enabled; //cache the value
   private Configuration configuration;
   private CacheNotifier notifier;
   private DistributionManager distributionManager;//is null for non-clustered caches

   @Inject
   public void injectDependencies(
                                    DataContainer dataContainer,
                                    Configuration configuration,
                                    CacheNotifier notifier,
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

    /*
    @Override
    public CacheEntry wrapEntryForReading(InvocationContext ctx, Object key) throws InterruptedException {
        return null;
    }
    */

    @Override
   public final CacheEntry wrapEntryForReading(InvocationContext ctx, Object key) throws InterruptedException {
      CacheEntry cacheEntry = this.getFromContext(ctx, key);
      if (cacheEntry == null) {
         cacheEntry = this.offHeapGetFromContainer(key, false);

         // do not bother wrapping though if this is not in a tx.  repeatable read etc are all meaningless unless there is a tx.
         if (useRepeatableRead) {
            MVCCEntry mvccEntry;
            if (cacheEntry == null) {
               mvccEntry = createWrappedEntry(key, null, ctx, null, false, false, false);
            } else {
               mvccEntry = createWrappedEntry(key, cacheEntry, ctx, null, false, false, false);
               // If the original entry has changeable state, copy state flags to the new MVCC entry.
               if (cacheEntry instanceof StateChangingEntry && mvccEntry != null)
                  mvccEntry.copyStateFlagsFrom((StateChangingEntry) cacheEntry);
            }

            if (mvccEntry != null) ctx.putLookedUpEntry(key, mvccEntry);
            if (trace) {
               log.tracef("Wrap %s for read. Entry=%s", key, mvccEntry);
            }
            return mvccEntry;
         } else if (cacheEntry != null) { // if not in transaction and repeatable read, or simply read committed (regardless of whether in TX or not), do not wrap
            ctx.putLookedUpEntry(key, cacheEntry);
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
   public final MVCCEntry wrapEntryForClear(InvocationContext ctx, Object key) throws InterruptedException {
      //skipRead == true because the keys values are not read during the ClearOperation (neither by application)
      MVCCEntry mvccEntry = this.offHeapWrapEntry(ctx, key, null, true);
      if (trace) {
         log.tracef("Wrap %s for clear. Entry=%s", key, mvccEntry);
      }
      return mvccEntry;
   }

    private MVCCEntry offHeapWrapEntry(InvocationContext ctx, Object key, Object o, boolean b) {
        return null;
    }


    private CacheEntry getFromContext(InvocationContext ctx, Object key) {
        return null;
    }

   @Override
   public final MVCCEntry wrapEntryForReplace(InvocationContext ctx, ReplaceCommand cmd) throws InterruptedException {
      Object key = cmd.getKey();
      MVCCEntry mvccEntry = this.offHeapWrapEntry(ctx, key, cmd.getMetadata(), false);
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
   public final MVCCEntry wrapEntryForRemove(InvocationContext ctx, Object key, boolean skipRead,
                                              boolean forInvalidation, boolean forceWrap) throws InterruptedException {
      CacheEntry cacheEntry = offHeapGetFromContext(ctx, key);
      MVCCEntry mvccEntry = null;
      if (cacheEntry != null) {
         if (cacheEntry instanceof MVCCEntry) {
            mvccEntry = (MVCCEntry) cacheEntry;
         } else {
            //skipRead == true because the key already exists in the context that means the key was previous accessed.
            mvccEntry = wrapMvccEntryForRemove(ctx, key, cacheEntry, true);
         }
      } else {
         InternalCacheEntry ice = offHeapFromContainer(key, forInvalidation);
         if (ice != null || clusterModeWriteSkewCheck || forceWrap) {
            mvccEntry = wrapInternalCacheEntryForPut(ctx, key, ice, null, skipRead);
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

    /*
    private MVCCEntry offHeapWrapMvccEntryForRemove(InvocationContext ctx, Object key, CacheEntry cacheEntry, boolean skipRead) {
        return null;
    }
    */

    private MVCCEntry wrapInternalCacheEntryForPut(InvocationContext ctx, Object key, InternalCacheEntry ice, Object o, boolean skipRead) {
        return null;
    }

    private InternalCacheEntry offHeapFromContainer(Object key, boolean forInvalidation) {
       return null;
    }

    @Override
   //removed final modifier to allow mock this method
   public MVCCEntry wrapEntryForPut(
            InvocationContext ctx,
            Object key,
            InternalCacheEntry icEntry,
            boolean undeleteIfNeeded,
            FlagAffectedCommand cmd,
            boolean skipRead)                throws InterruptedException {
      CacheEntry cacheEntry = offHeapGetFromContext(ctx, key);
      MVCCEntry mvccEntry;
      if (cacheEntry != null && cacheEntry.isNull() && !useRepeatableRead) cacheEntry = null;
      Metadata providedMetadata = cmd.getMetadata();
      if (cacheEntry != null) {
         if (useRepeatableRead) {
            //sanity check. In repeatable read, we only deal with RepeatableReadEntry and ClusteredRepeatableReadEntry
            if (cacheEntry instanceof RepeatableReadEntry) {
               mvccEntry = (MVCCEntry) cacheEntry;
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
         InternalCacheEntry ice = (icEntry == null ? offHeapGetFromContainer(key, false) : icEntry);
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
             this.wrapInternalCacheEntryForPut(ctx, key, ice, providedMetadata, skipRead) :
             this.newMvccEntryForPut(ctx, key, cmd, providedMetadata, skipRead);
      }
      mvccEntry.copyForUpdate(container);
      if (trace) {
         log.tracef("Wrap %s for put. Entry=%s", key, mvccEntry);
      }
      return mvccEntry;
   }
/*
    @Override
    public CacheEntry wrapEntryForDelta(InvocationContext ctx, Object deltaKey, Delta delta) throws InterruptedException {
        return null;
    }

    @Override
    public MVCCEntry wrapEntryForPut(InvocationContext ctx, Object key, InternalCacheEntry ice, boolean undeleteIfNeeded, FlagAffectedCommand cmd, boolean skipRead) throws InterruptedException {
        return null;
    }
    */

    private MVCCEntry offHeapNewMvccEntryForPut(InvocationContext ctx, Object key, OffHeapFlagAffectedCommand cmd, Metadata providedMetadata, boolean skipRead) {
        return null;
    }

    private void offHeapUpdateMetadata(MVCCEntry mvccEntry, Metadata providedMetadata) {

    }

    private MVCCEntry offHeapNewMvccEntryForPut(
                                                        InvocationContext ctx,
                                                        Object key, FlagAffectedCommand cmd,
                                                        Metadata providedMetadata,
                                                        boolean skipRead) {
        return null;
    }


    private void offHeapUpdateVersion(MVCCEntry mvccEntry, Metadata metadata) {
    }

   @Override
   public CacheEntry wrapEntryForDelta(
           InvocationContext ctx,
           Object deltaKey,
           Delta delta)                           throws InterruptedException {

        CacheEntry cacheEntry = offHeapGetFromContext(ctx, deltaKey);
      DeltaAwareCacheEntry deltaAwareEntry = null;
      if (cacheEntry != null) {        
//         deltaAwareEntry =  offHeapWrapInternalCacheEntryForPut(
//                  OffHeapInvocationContext,
//                  Object ,
//                  OffHeapInternalCacheEntry ,
//                  OffHeapMetadata ,
//                  boolean ) {} tryForDelta(ctx, deltaKey, cacheEntry);
      } else {                     
         InternalCacheEntry ice = this.offHeapFromContainer(deltaKey, false);
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
   
   private DeltaAwareCacheEntry wrapEntryForDelta(InvocationContext ctx, Object key, CacheEntry cacheEntry) {
      if (cacheEntry instanceof DeltaAwareCacheEntry) return (DeltaAwareCacheEntry) cacheEntry;
      return wrapInternalCacheEntryForDelta(ctx, key, cacheEntry);
   }

    private DeltaAwareCacheEntry offHeapWrapInternalCacheEntryForDelta(
                                                                        InvocationContext ctx,
                                                                        Object key,
                                                                        CacheEntry cacheEntry) {
        return null;
    }

    private DeltaAwareCacheEntry wrapInternalCacheEntryForDelta(
                                                    InvocationContext ctx,
                                                    Object key,
                                                    CacheEntry cacheEntry) {
      DeltaAwareCacheEntry e;
      if(cacheEntry instanceof MVCCEntry){
         e = createWrappedDeltaEntry(key, (DeltaAware) cacheEntry.getValue(), cacheEntry);
      }
      else if (cacheEntry instanceof InternalCacheEntry) {
         cacheEntry = wrapInternalCacheEntryForPut(ctx, key, (InternalCacheEntry) cacheEntry, null, false);
         e = createWrappedDeltaEntry(key, (DeltaAware) cacheEntry.getValue(), cacheEntry);
      }
      else {
         e = createWrappedDeltaEntry(key, (DeltaAware) cacheEntry.getValue(), null);
      }
      ctx.putLookedUpEntry(key, e);
      return e;

   }

    /*
    private CacheEntry offHeapWrapInternalCacheEntryForPut(
                                                    InvocationContext ctx,
                                                    Object key,
                                                    InternalCacheEntry cacheEntry,
                                                    Object o,
                                                    boolean b) {
        return null;
    }
    */

    private CacheEntry offHeapGetFromContext(InvocationContext ctx, Object key) {
      final CacheEntry cacheEntry = ctx.lookupEntry(key);
      if (trace) log.tracef("Exists in context? %s ", cacheEntry);
      return cacheEntry;
   }

   private InternalCacheEntry offHeapGetFromContainer(Object key, boolean forceFetch) {
      final boolean isLocal = distributionManager == null || distributionManager.getLocality(key).isLocal();
      final InternalCacheEntry ice = isL1Enabled || isLocal || forceFetch ? container.get(key) : null;
      if (trace) log.tracef("Retrieved from container %s (isL1Enabled=%s, isLocal=%s)", ice, isL1Enabled, isLocal);
      return ice;
   }

   private MVCCEntry newMvccEntryForPut(
                                    InvocationContext ctx,
                                    Object key,
                                    FlagAffectedCommand cmd,
                                    Metadata providedMetadata,
                                    boolean skipRead) {
      MVCCEntry mvccEntry;
      if (trace) log.trace("Creating new entry.");
      Object v=null;
      boolean tf = true;
      this.notifier.notifyCacheEntryCreated(key, v, tf, ctx, cmd);
      mvccEntry = this.offHeapCreateWrappedEntry(key, null, ctx, providedMetadata, true, false, skipRead);
      mvccEntry.setCreated(true);
      ctx.putLookedUpEntry(key, mvccEntry);
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
    private MVCCEntry offHeapCreateWrappedEntry(
                                                    Object key,
                                                    Object o,
                                                    InvocationContext ctx,
                                                    Metadata providedMetadata,
                                                    boolean b,
                                                    boolean b1,
                                                    boolean skipRead) {
        return null;
    }

    private MVCCEntry offHeapWrapMvccEntryForPut(
                                                    InvocationContext ctx,
                                                    Object key,
                                                    CacheEntry cacheEntry,
                                                    Metadata providedMetadata,
                                                    boolean skipRead) {
      if (cacheEntry instanceof MVCCEntry) {
         MVCCEntry mvccEntry = (MVCCEntry) cacheEntry;
         this.offHeapUpdateMetadata(mvccEntry, providedMetadata);
         return mvccEntry;
      }
      return wrapInternalCacheEntryForPut(
              ctx,
              key,
              (InternalCacheEntry) cacheEntry,
              providedMetadata,
              skipRead);
   }

   private MVCCEntry wrapInternalCacheEntryForPut(
                                                        InvocationContext ctx,
                                                        Object key,
                                                        InternalCacheEntry cacheEntry,
                                                        Metadata providedMetadata,
                                                        boolean skipRead) {
      MVCCEntry mvccEntry = offHeapCreateWrappedEntry(key, cacheEntry, ctx, providedMetadata, true, false, skipRead);
      ctx.putLookedUpEntry(key, mvccEntry);
      return mvccEntry;
   }

   private MVCCEntry wrapMvccEntryForRemove(InvocationContext ctx,
                                                          Object key,
                                                          CacheEntry cacheEntry,
                                                          boolean skipRead) {
      MVCCEntry mvccEntry = offHeapCreateWrappedEntry(key, cacheEntry, ctx, null, false, true, skipRead);
      // If the original entry has changeable state, copy state flags to the new MVCC entry.
      if (cacheEntry instanceof StateChangingEntry)
         mvccEntry.copyStateFlagsFrom((StateChangingEntry) cacheEntry);

      ctx.putLookedUpEntry(key, mvccEntry);
      return mvccEntry;
   }

   private MVCCEntry wrapEntry(InvocationContext ctx, Object key, Metadata providedMetadata, boolean skipRead) {
      CacheEntry cacheEntry = offHeapGetFromContext(ctx, key);
      MVCCEntry mvccEntry = null;
      if (cacheEntry != null) {
         //already wrapped. set skip read to true to avoid replace the current version.
         mvccEntry = offHeapWrapMvccEntryForPut(ctx, key, cacheEntry, providedMetadata, true);
      } else {
         InternalCacheEntry ice = offHeapGetFromContainer(key, false);
         if (ice != null || clusterModeWriteSkewCheck) {
            mvccEntry = this.wrapInternalCacheEntryForPut(ctx, key, ice, providedMetadata, skipRead);
         }
      }
      if (mvccEntry != null)
         mvccEntry.copyForUpdate(container);
      return mvccEntry;
   }

    /*
    private MVCCEntry wrapInternalCacheEntryForPut(InvocationContext ctx, Object key, InternalCacheEntry ice, Metadata providedMetadata, boolean skipRead) {
        return null;
    }
    */

    protected MVCCEntry createWrappedEntry(
                                            Object key,
                                            CacheEntry cacheEntry,
                                            InvocationContext context,
                                            Metadata providedMetadata,
                                            boolean isForInsert,
                                            boolean forRemoval,
                                            boolean skipRead) {
      Object value = cacheEntry != null ? cacheEntry.getValue() : null;
      Metadata metadata = providedMetadata != null
            ? providedMetadata
            : cacheEntry != null ? cacheEntry.getMetadata() : null;

      if (value == null && !isForInsert && !useRepeatableRead)
         return null;

      return useRepeatableRead
            ? new ClusteredRepeatableReadEntry(key, value, metadata)
            : new ClusteredRepeatableReadEntry(key, value, metadata); //yes, we know its a placeholder.
   }
   
   private DeltaAwareCacheEntry newDeltaAwareCacheEntry(InvocationContext ctx, Object key, DeltaAware deltaAware){
      DeltaAwareCacheEntry deltaEntry = this.offHeapCreateWrappedDeltaEntry(key, deltaAware, null);
      ctx.putLookedUpEntry(key, deltaEntry);
      return deltaEntry;
   }

    private DeltaAwareCacheEntry offHeapCreateWrappedDeltaEntry(Object key, DeltaAware deltaAware, Object o) {
        return null;
    }

    private DeltaAwareCacheEntry createWrappedDeltaEntry(Object key, DeltaAware deltaAware, CacheEntry entry) {
      return new DeltaAwareCacheEntry(key,deltaAware, entry);
   }

   private void updateMetadata(MVCCEntry entry, Metadata providedMetadata) {
      if (trace) {
         log.tracef("Update metadata for %s. Provided metadata is %s", entry, providedMetadata);
      }
      if (providedMetadata == null || entry == null || entry.getMetadata() != null) {
         return;
      }
      entry.setMetadata(providedMetadata);
   }

   private void updateVersion(MVCCEntry entry, Metadata providedMetadata) {
      if (trace) {
         log.tracef("Update metadata for %s. Provided metadata is %s", entry, providedMetadata);
      }
      if (providedMetadata == null || entry == null) {
         return;
      } else if (entry.getMetadata() == null) {
         entry.setMetadata(providedMetadata);
         return;
      }

      entry.setMetadata(Metadatas.applyVersion(entry.getMetadata(), providedMetadata));
   }

}
