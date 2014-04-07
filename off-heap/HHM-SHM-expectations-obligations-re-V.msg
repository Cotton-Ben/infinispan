
From: Peter Lawrey [mailto:peter.lawrey@higherfrequencytrading.com] 
Sent: Thursday, April 03, 2014 11:17 AM
To: Cotton, Ben
Subject: Re: HHM/SHM expectations/obligations re: V

The minimum requirement is that the key and value are Serializable. Note: Object is not Serializable which some people get confused about. ;)

Serializable is to be avoided of course,  Externalizable or ByteMarshallable is better for keys and values.  Some built in types have their own serializers and you can implement your own for a custom type but I don't know of anyone who uses this feature.

If you want zero copy, you can do this for value (but not keys) and the minimum for this is Byteable.  If you want the library to generate a Byteable for you, you need to followed conventions based on Java Beans. In this case you can have nested types or array like elements via getXxxAt and setXxxAt, provided they are also generated, String or primitive.  If you implement your own Byteable, you can do what you like with it, provided you fit inside the entrySize() you built the collection with.

There could be provision for Map, List, Set, Queue, Deque, but in the short term that would involve serialization/deserialization.

On 3 April 2014 15:44, Cotton, Ben <ben.cotton@jpmorgan.com> wrote:
Hi Peter,

Sorry for the flurry of terribly worded public questions at https://github.com/OpenHFT/HugeCollections/issues/13  …. I was on the bus, typing it all in on a phone w/o  my glasses, and I get excited (and then worried) to be as clear as possible.  My bad.
 
Would you mind providing to me, a general statement of  specific expectation/obligation for HHM/SHM <K,V> capability?
 
I mean based on what I know via our general 3.0e experience and exercising  BondVOInterface (as via the InfoQ samples), is the following completely true:  
 
1.       As long as we provide an interface graph (i.e. Neseted interfaces supported) that is at all points JBI compliant, We can use such an interface graph as a V in HHM/SHM
2.      All JBI gettr/settr fields in the Interface graph must be in typeSet = {String, {any primitive}}      (i.e – I can’t have somehing like a sub-Map as part of V in SHM/HHM)
3.      If both 1 & 2 are completely true, then we can then safely have the expectation that OpenHFT will interoperate soundly/completely as a ConcurrentMap – with both ZC and Externalizable accommodations for V.  
 
Is the above 3 close to being sound/complete?
 
If not, could you assist us to provide a sound/complete  statement approx =   “Here is your HHM,SHM V obligation,  if you meet it, here is your valid expectation for HHM,SHM capability wrt to V” ?
 
i.e. I think we need what I would call the official  OpenHFT SHM/HHM “terms for endearment” wrt to V.
 
Very grateful, thank you Peter.
 
 
 
 
 
Ben D. Cotton III
J.P.Morgan
Liquidity Risk Technology
277 Park Ave  Desk 08-GG64
New York, NY 10172-0003
212.622.5010
ben.cotton@jpmorgan.com
 
 
This email is confidential and subject to important disclaimers and conditions including on offers for the purchase or sale of securities, accuracy and completeness of information, viruses, confidentiality, legal privilege, and legal entity disclaimers, available at http://www.jpmorgan.com/pages/disclosures/email. 

