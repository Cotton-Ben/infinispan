package org.infinispan.offheap.container;

import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.Metadatas;
import org.infinispan.offheap.container.entries.CacheEntry;
import org.infinispan.offheap.container.entries.ClusteredRepeatableReadEntry;
import org.infinispan.offheap.container.entries.MVCCEntry;
import org.infinispan.offheap.container.versioning.VersionGenerator;

/**
 * An entry factory that is capable of dealing with SimpleClusteredVersions.  This should <i>only</i> be used with
 * optimistically transactional, repeatable read, write skew check enabled caches in replicated or distributed mode.
 *
 * @author Manik Surtani
 * @since 5.1
 */
public class IncrementalVersionableEntryFactoryImpl extends EntryFactoryImpl {

   private VersionGenerator versionGenerator;

   @Start (priority = 9)
   public void setWriteSkewCheckFlag() {
      useRepeatableRead = true;
   }

   @Inject
   public void injectVersionGenerator(VersionGenerator versionGenerator) {
      this.versionGenerator = versionGenerator;
   }

   @Override
   protected MVCCEntry createWrappedEntry(Object key, CacheEntry cacheEntry, InvocationContext context,
                                          Metadata providedMetadata, boolean isForInsert, boolean forRemoval, boolean skipRead) {
      Metadata metadata;
      Object value;
      if (cacheEntry != null) {
         value = cacheEntry.getValue();
         Metadata entryMetadata = cacheEntry.getMetadata();
         if (providedMetadata != null && entryMetadata != null) {
            metadata = Metadatas.applyVersion(entryMetadata, providedMetadata);
         } else if (providedMetadata == null) {
            metadata = entryMetadata; // take the metadata in memory
         } else {
            metadata = providedMetadata;
         }
         if (context.isOriginLocal() && context.isInTxScope()) {
            ((TxInvocationContext) context).getCacheTransaction().addVersionRead(key, skipRead ? null : metadata.version());
         }
      } else {
         value = null;
         metadata = providedMetadata == null ? new EmbeddedMetadata.Builder().version(versionGenerator.nonExistingVersion()).build()
               : providedMetadata;
         if (context.isOriginLocal() && context.isInTxScope()) {
            ((TxInvocationContext) context).getCacheTransaction().addVersionRead(key, skipRead ? null : versionGenerator.nonExistingVersion());
         }
      }

      //only the ClusteredRepeatableReadEntry are used, even to represent the null values.
      return new ClusteredRepeatableReadEntry(key, value, metadata);
   }

}
