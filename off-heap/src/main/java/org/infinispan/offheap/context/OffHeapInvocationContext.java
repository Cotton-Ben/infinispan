package org.infinispan.offheap.context;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.context.EntryLookup;
import org.infinispan.offheap.container.entries.OffHeapCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapMVCCEntry;
import org.infinispan.remoting.transport.Address;

import java.util.Set;

/**
 * A context that contains information pertaining to a given invocation.  These contexts typically have the lifespan of
 * a single invocation.
 *
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public interface OffHeapInvocationContext extends OffHeapEntryLookup, Cloneable {

   /**
    * Returns true if the call was originated locally, false if it is the result of a remote rpc.
    */
   boolean isOriginLocal();
   
   /**
    * Get the origin of the command, or null if the command originated locally
    * @return
    */
   Address getOrigin();

   /**
    * Returns true if this call is performed in the context of an transaction, false otherwise.
    */
   boolean isInTxScope();

   /**
    * Returns the in behalf of which locks will be aquired.
    */
   Object getLockOwner();

   /**
    * Clones the invocation context.
    *
    * @return A cloned instance of this invocation context instance
    */
   OffHeapInvocationContext clone();

   /**
    * Returns the set of keys that are locked for writing.
    */
   Set<Object> getLockedKeys();

   void clearLockedKeys();

   /**
    * Returns the class loader associated with this invocation
    *
    * @return a class loader instance or null if no class loader was
    *         specifically associated
    */
   ClassLoader getClassLoader();

   /**
    * Sets the class loader associated for this invocation
    *
    * @param classLoader
    */
   void setClassLoader(ClassLoader classLoader);

   /**
    * Tracks the given key as locked by this invocation context.
    */
   void addLockedKey(Object key);

   /**
    * Returns true if the lock being tested is already held in the current scope, false otherwise.
    *
    * @param key lock to test
    */
   boolean hasLockedKey(Object key);

   /**
    * Tries to replace the value of the wrapped entry associated with the given key in the context, if one exists.
    *
    * @return true if the context already contained a wrapped entry for which this value was changed, false otherwise.
    */
   boolean replaceValue(Object key, InternalCacheEntry cacheEntry);

   boolean isEntryRemovedInContext(Object key);


    void offHeapPutLookedUpEntry(Object key, OffHeapMVCCEntry mvccEntry);

    void offHeapPutLookedUpEntry(Object key, OffHeapCacheEntry cacheEntry);
}
