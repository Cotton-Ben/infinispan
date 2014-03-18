package org.infinispan.offheap.commons.marshall;

import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * Base class for {@link org.infinispan.commons.marshall.AdvancedExternalizer} implementations that offers default
 * implementations for some of its methods. In particular, this base class
 * offers a default implementation for {@link org.infinispan.commons.marshall.AdvancedExternalizer#getId()}
 * that returns null which is particularly useful for advanced externalizers
 * whose id will be provided by XML or programmatic configuration rather than
 * the externalizer implementation itself.
 *
 * @author Galder Zamarreño
 * @since 5.0
 */
public abstract class OffHeapAbstractExternalizer<T> implements AdvancedExternalizer<T> {

   @Override
   public Integer getId() {
      return null;
   }

    /**
     * Indexes for object types included in commons.
     *
     * @author Galder Zamarreño
     * @since 6.0
     */
    public static interface OffHeapIds {
       int MURMURHASH_2 = 71;
       int MURMURHASH_2_COMPAT = 72;
       int MURMURHASH_3 = 73;

       int EMPTY_SET = 88;
       int EMPTY_MAP = 89;
       int EMPTY_LIST = 90;
       // internal collections (id=18 no longer in use, might get reused at a later stage)
       int IMMUTABLE_MAP = 19;
       int BYTE_BUFFER = 106;
       int METADATA_TRANSIENT_MORTAL_ENTR = 29;
    }
}
