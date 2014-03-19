package org.infinispan.offheap.container;

import org.infinispan.atomic.Delta;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.offheap.container.entries.OffHeapCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapMVCCEntry;
import org.infinispan.offheap.context.OffHeapInvocationContext;


/**
 * A factory for constructing {@link org.infinispan.offheap.container.entries.OffHeapMVCCEntry} instances
 * for use in the {@link org.infinispan.context.InvocationContext}.
 * Implementations of this interface would typically wrap an internal {@link org.infinispan.offheap.container.entries.OffHeapCacheEntry}
 * with an {@link org.infinispan.offheap.container.entries.OffHeapMVCCEntry}, optionally acquiring the necessary locks via the
 * {@link org.infinispan.util.concurrent.locks.LockManager}.
 *
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @author Galder Zamarreño
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public interface OffHeapEntryFactory {

   /**
    * Wraps an entry for reading.  Usually this is just a raw {@link org.infinispan.offheap.container.entries.OffHeapCacheEntry}
    * but certain combinations of isolation
    * levels and the presence of an ongoing JTA transaction may force this to be a proper, wrapped MVCCEntry.  The entry
    * is also typically placed in the invocation context.
    *
    * @param ctx current invocation context
    * @param key key to look up and wrap
    * @throws InterruptedException when things go wrong, usually trying to acquire a lock
    */
   OffHeapCacheEntry wrapEntryForReading(InvocationContext ctx, Object key) throws InterruptedException;

    OffHeapCacheEntry wrapEntryForReading(OffHeapInvocationContext ctx, Object key) throws InterruptedException;

    /**
    * Used for wrapping individual keys when clearing the cache. The wrapped entry is added to the
    * supplied InvocationContext.
    */
   OffHeapMVCCEntry wrapEntryForClear(InvocationContext ctx, Object key) throws InterruptedException;

   /**
    * Used for wrapping a cache entry for replacement. The wrapped entry is added to the
    * supplied InvocationContext.
    */
   OffHeapMVCCEntry wrapEntryForReplace(InvocationContext ctx, ReplaceCommand cmd) throws InterruptedException;

   /**
    * Used for wrapping a cache entry for removal. The wrapped entry is added to the supplied InvocationContext.
    *
    * @param skipRead if {@code true}, if the key is not read during the remove operation. Only used with Repeatable
    *                 Read + Write Skew + Versioning + Cluster.
    */
   OffHeapMVCCEntry wrapEntryForRemove(InvocationContext ctx, Object key, boolean skipRead, boolean forInvalidation,
                                boolean forceWrap) throws InterruptedException;

    //removed final modifier to allow mock this method
    OffHeapMVCCEntry wrapEntryForPut(
            OffHeapInvocationContext ctx,
            Object key,
            OffHeapInternalCacheEntry icEntry,
            boolean undeleteIfNeeded,
            OffHeapFlagAffectedCommand cmd,
            boolean skipRead)                throws InterruptedException;

    /**
    * Used for wrapping Delta entry to be applied to DeltaAware object stored in cache. The wrapped
    * entry is added to the supplied InvocationContext.
    */
   OffHeapCacheEntry wrapEntryForDelta(InvocationContext ctx, Object deltaKey, Delta delta) throws InterruptedException;

   /**
    * Used for wrapping a cache entry for addition to cache. The wrapped entry is added to the supplied
    * InvocationContext.
    *
    * @param skipRead if {@code true}, if the key is not read during the put operation. Only used with Repeatable Read +
    *                 Write Skew + Versioning + Cluster.
    */
   OffHeapMVCCEntry wrapEntryForPut(InvocationContext ctx, Object key, OffHeapInternalCacheEntry ice,
                             boolean undeleteIfNeeded, FlagAffectedCommand cmd, boolean skipRead) throws InterruptedException;

    OffHeapCacheEntry wrapEntryForDelta(
            OffHeapInvocationContext ctx,
            Object deltaKey,
            Delta delta)                           throws InterruptedException;
}
