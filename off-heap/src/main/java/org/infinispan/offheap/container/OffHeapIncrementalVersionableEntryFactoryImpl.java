package org.infinispan.offheap.container;

import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.container.entries.OffHeapClusteredRepeatableReadEntry;
import org.infinispan.offheap.context.OffHeapInvocationContext;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadatas;
import org.infinispan.offheap.container.entries.OffHeapCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapMVCCEntry;
import org.infinispan.offheap.container.versioning.OffHeapVersionGenerator;
import org.infinispan.offheap.metadata.OffHeapMetadata;
import org.infinispan.offheap.metadata.OffHeapMetadatas;

/**
 * An entry factory that is capable of dealing with SimpleClusteredVersions.  This should <i>only</i> be used with
 * optimistically transactional, repeatable read, write skew check enabled caches in replicated or distributed mode.
 *
 * @author Manik Surtani
 * @since 5.1
 */
public class OffHeapIncrementalVersionableEntryFactoryImpl extends OffHeapEntryFactoryImpl {

   private OffHeapVersionGenerator versionGenerator;

   @Start (priority = 9)
   public void setWriteSkewCheckFlag() {
      useRepeatableRead = true;
   }

   @Inject
   public void injectVersionGenerator(OffHeapVersionGenerator versionGenerator) {
      this.versionGenerator = versionGenerator;
   }

   @Override
   protected OffHeapMVCCEntry createWrappedEntry(
                                        Object key,
                                        OffHeapCacheEntry cacheEntry,
                                        OffHeapInvocationContext context,
                                        OffHeapMetadata providedMetadata,
                                        boolean isForInsert,
                                        boolean forRemoval,
                                        boolean skipRead) {
      OffHeapMetadata metadata;
      Object value;
      if (cacheEntry != null) {
         value = cacheEntry.getValue();
         OffHeapMetadata entryMetadata = cacheEntry.getMetadata();
         if (providedMetadata != null && entryMetadata != null) {
            metadata = OffHeapMetadatas.applyVersion(entryMetadata, providedMetadata);
         } else if (providedMetadata == null) {
            metadata = entryMetadata; // take the metadata in memory
         } else {
            metadata = providedMetadata;
         }
         if (context.isOriginLocal() && context.isInTxScope()) {
             //ben.cotton@jpmorgan.com  OpenHFT SHM does not support ACID transactions at the moment.
            ((TxInvocationContext) context).getCacheTransaction().addVersionRead(
                                                            key,
                                                            skipRead ? null : null/* metadata.version() */);
         }
      } else {
         value = null;
         metadata = providedMetadata == null ? new OffHeapEmbeddedMetadata
                                                            .OffHeapBuilder()
                                                            .version(versionGenerator.nonExistingVersion())
                                                            .build()
               : providedMetadata;
         if (context.isOriginLocal() && context.isInTxScope()) {
            //((TxInvocationContext) context).getCacheTransaction().addVersionRead(key, skipRead ? null : versionGenerator.nonExistingVersion());
            //ben.cotton@jpmorgan.com  OpenHFT SHM does not support ACID transactions at the moment.
            ((TxInvocationContext) context).getCacheTransaction().addVersionRead(
                     key,
                     skipRead ? null : null/* versionGenerator.nonExistingVersion() */);

         }
      }

      //only the ClusteredRepeatableReadEntry are used, even to represent the null values.
      return new OffHeapClusteredRepeatableReadEntry(key, value, metadata);
   }

}
