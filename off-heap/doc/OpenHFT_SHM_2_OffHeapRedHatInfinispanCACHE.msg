


-----Original Message-----
From: Cotton, Ben 
Sent: Wednesday, April 02, 2014 2:38 PM
To: Mircea Markus
Cc: Wang, Xiaoming; Gordeev, Dmitry; peter.lawrey@higherfrequencytrading.com; Tristan Tarrant; Dildy, Justin P; anistor@redhat.com; ben.cotton@alumni.rutgers.edu
Subject: RE: build bridge from Off-Heap SHM to ISPN 7 ICE (via DataContainer)

Thanks Mircea. Give us one more day, before we formally elaborate on the road block (we are not perfectly sure ourselves).

What we ultimately want to do is build an ISPN7-valid Test of a new bridge that connects a JCACHE API view to an HPC off-heap capability.

Specifically:

from
1.  Cache.Entry<K,V> API view     (ISPN7 AdvancedCache CacheImpl) to
2.  DataContainer<K,V> API view   (JPM OffHeapDefaultDataContainer<K,V> Impl) to
3.  SharedHashMap<K,V> API view   (OpenHFT VanillaSharedHashMap<K,V> Impl)

We want to model the planned off-heap bridge described above, based off of the symmetry any *exisiting* Red-Hat test that proves an inplace existing on-heap bridge from

1.  Cache.Entry<K,V> API view     (ISPN7 AdvancedCache CacheImpl) to
2.  DataContainer<K,V> API view   (RedHat DefaultDataContainer Impl) to
3.  ConcurrentMap<K,V> API view   (RedHat BoundedConcurrentHashMap Impl)

The potential roadblock that has us mildly concerned is that neither of

DataContainer nor DefaultDataContainer are explicilty generic on <K,V> and instead binds as <Object,InternalCacheEntry>. Interestingly, BoundedConcurrentMap is generic on <K,V> (but the current bridge to it may not take adavantage of that). This DataContainer binding to something that is not formally <K,V> may be a problem with our ultimately, generically, building a best bridge to the off-heap capability.  

Today's meeting at JPM is to explore this concern and identify our view of it as an (isA|isNotA) potential roadblock.

Will get back to you tomorrow w update.

-----Original Message-----
From: Mircea Markus [mailto:mmarkus@redhat.com] 
Sent: Wednesday, April 02, 2014 1:57 PM
To: Cotton, Ben
Cc: Wang, Xiaoming; Gordeev, Dmitry; peter.lawrey@higherfrequencytrading.com; Tristan Tarrant; Dildy, Justin P; anistor@redhat.com
Subject: Re: build bridge from Off-Heap SHM to ISPN 7 ICE (via DataContainer)

Thanks for sharing this Ben; adding Adrian Nistor to the chain as he's the one that will overview the integration from an Infinispan perspective.

Can you please elaborate on what the roadblock is?  

On Apr 2, 2014, at 18:34, Cotton, Ben <ben.cotton@jpmorgan.com> wrote:

> When: Wednesday, April 02, 2014 4:00 PM-4:30 PM (UTC-05:00) Eastern Time (US & Canada).
> Where: Ben, Dmitry, Xiao desks
>  
> Note: The GMT offset above does not reflect daylight saving time adjustments.
>  
> *~*~*~*~*~*~*~*~*~*
>  
> Dmitry,Xiao:  Here is the current ROADBLOCK issue.  This meeting’s objective will be to analyze the options for remedy.
>  
> (Peter,Mircea,Tristan,Justin: this meeting notice is just FYI)
>  
> RedHat has the following @Test of their on-heap DefaultDataContainer.
>  
> https://github.com/Cotton-Ben/infinispan/blob/master/core/src/test/java/org/infinispan/container/SimpleDataContainerTest.java
>  
> The RedHat DataContainer API is what JPM will use as the basis of connecting OpenHFT’s operand=SharedHashMap.Entry (off-heap) to ISPN 7’s  operand=InternalCacheEntry (on-heap).
>  
> To affect this, Dmitry,Xiao, and I will build a new OffHeapDefaultDataContainer (implements DataContainer) that can pass an off-heap (version of the above RedHat) Test at
>  
> https://github.com/Cotton-Ben/infinispan/blob/master/off-heap/src/test/java/org/infinispan/offheap/OffHeapDefaultDataContainerTest.java
>  
> The current roadblock is that  what ISPN7 currently does do via DefaultDataContainer.java:
>  
> ConcurrentMap<Object,InternalCacheEntry> entries = new BoundedConcurrentHashMap<Object,InternalCacheEntry>(/**/);
>  
> We cannot do via OffDefaultDataContainer.java:
>  
> SharedHashMap<Object, InternalCacheEntry> entries =
>                    (SharedHashMap<Object, InternalCacheEntry>)  new SharedHashMapBuilder(/**/);
>  
>  
> Musing openly, I think we may have to adapt some notion of a “DataContainer Visitor” capability within any candidate solution.
>  
>  
>  
> Ben D. Cotton III
> J.P.Morgan
> Liquidity Risk Technology
> 277 Park Ave  Desk 08-GG64
> New York, NY 10172-0003
> 212.622.5010
> ben.cotton@jpmorgan.com
>  
>  
>  
>  
>  
>  
>  
> This email is confidential and subject to important disclaimers and conditions including on offers for the purchase or sale of securities, accuracy and completeness of information, viruses, confidentiality, legal privilege, and legal entity disclaimers, available athttp://www.jpmorgan.com/pages/disclosures/email.
> 
> <Mail Attachment.ics>

Cheers,
-- 
Mircea Markus
Infinispan lead (www.infinispan.org)




