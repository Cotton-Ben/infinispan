package org.infinispan.offheap;

import org.infinispan.offheap.container.OffHeapDataContainer;
import org.infinispan.offheap.container.OffHeapDefaultDataContainer;
import org.infinispan.container.InternalEntryFactoryImpl;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.offheap.container.entries.*;
import org.infinispan.offheap.metadata.OffHeapEmbeddedMetadata;
import org.infinispan.offheap.util.OffHeapCoreImmutables;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.offheap.util.CoreImmutables;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;


/**
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 *
 * modeled from RedHat's original SimpleDataContainerTest.java
 */


@Test(groups = "unit", testName = "offheap.OffHeapDataContainerTest")
public class OffHeapDefaultDataContainerTest extends AbstractInfinispanTest {
    OffHeapDataContainer dc;

    @BeforeMethod
    public void setUp() {
        dc = createContainer();
    }

    @AfterMethod
    public void tearDown() {
        dc = null;
    }



    protected OffHeapDataContainer createContainer() {
        OffHeapDefaultDataContainer dc = new OffHeapDefaultDataContainer(
                           String.class,
                           BondVOInterface.class,
                           "BondVoOperand",
                           512,
                           256
                        );
        InternalEntryFactoryImpl internalEntryFactory = new InternalEntryFactoryImpl();
        internalEntryFactory.injectTimeService(TIME_SERVICE);
        dc.initialize(
                null, null, internalEntryFactory, null, null, TIME_SERVICE
        );
        return dc;
    }


    public void testExpiredData() throws InterruptedException {
        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        Thread.sleep(100);

        OffHeapInternalCacheEntry entry = dc.get("k");
        assert entry.getClass().equals(transienttype());
        assert entry.getLastUsed() <= System.currentTimeMillis();
        long entryLastUsed = entry.getLastUsed();
        Thread.sleep(100);
        entry = dc.get("k");
        assert entry.getLastUsed() > entryLastUsed;
        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(0, TimeUnit.MINUTES).build());
        dc.purgeExpired();

        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        Thread.sleep(100);
        assert dc.size() == 1;

        entry = dc.get("k");
        assert entry != null : "Entry should not be null!";
        assert entry.getClass().equals(mortaltype()) : "Expected "+mortaltype()+", was " + entry.getClass().getSimpleName();
        assert entry.getCreated() <= System.currentTimeMillis();

        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(0, TimeUnit.MINUTES).build());
        Thread.sleep(10);
        assert dc.get("k") == null;
        assert dc.size() == 0;

        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(0, TimeUnit.MINUTES).build());
        Thread.sleep(100);
        assert dc.size() == 1;
        dc.purgeExpired();
        assert dc.size() == 0;
    }


    public void testResetOfCreationTime() throws Exception {
        long now = System.currentTimeMillis();
        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(1000, TimeUnit.SECONDS).build());
        long created1 = dc.get("k").getCreated();
        assert created1 >= now;
        Thread.sleep(100);
        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(1000, TimeUnit.SECONDS).build());
        long created2 = dc.get("k").getCreated();
        assert created2 > created1 : "Expected " + created2 + " to be greater than " + created1;
    }


    public void testUpdatingLastUsed() throws Exception {
        long idle = 600000;
        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        OffHeapInternalCacheEntry ice = dc.get("k");
        assert ice.getClass().equals(immortaltype());
        assert ice.toInternalCacheValue().getExpiryTime() == -1;
        assert ice.getMaxIdle() == -1;
        assert ice.getLifespan() == -1;
        dc.put("k", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(idle, TimeUnit.MILLISECONDS).build());
        long oldTime = System.currentTimeMillis();
        Thread.sleep(100); // for time calc granularity
        ice = dc.get("k");
        assert ice.getClass().equals(transienttype());
        assert ice.toInternalCacheValue().getExpiryTime() > -1;
        assert ice.getLastUsed() > oldTime;
        Thread.sleep(100); // for time calc granularity
        assert ice.getLastUsed() < System.currentTimeMillis();
        assert ice.getMaxIdle() == idle;
        assert ice.getLifespan() == -1;

        oldTime = System.currentTimeMillis();
        Thread.sleep(100); // for time calc granularity
        assert dc.get("k") != null;

        // check that the last used stamp has been updated on a get
        assert ice.getLastUsed() > oldTime;
        Thread.sleep(100); // for time calc granularity
        assert ice.getLastUsed() < System.currentTimeMillis();
    }

    protected Class<? extends OffHeapInternalCacheEntry> mortaltype() {
        return OffHeapMortalCacheEntry.class;
    }

    protected Class<? extends OffHeapInternalCacheEntry> immortaltype() {
        return OffHeapImmortalCacheEntry.class;
    }

    protected Class<? extends OffHeapInternalCacheEntry> transienttype() {
        return OffHeapTransientCacheEntry.class;
    }

    protected Class<? extends OffHeapInternalCacheEntry> transientmortaltype() {
        return OffHeapTransientMortalCacheEntry.class;
    }


    public void testExpirableToImmortalAndBack() {
        String value = "v";
        dc.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        assertContainerEntry(this.mortaltype(), value);

        value = "v2";
        dc.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        assertContainerEntry(this.immortaltype(), value);

        value = "v3";
        dc.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        assertContainerEntry(this.transienttype(), value);

        value = "v4";
        dc.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .lifespan(100, TimeUnit.MINUTES).maxIdle(100, TimeUnit.MINUTES).build());
        assertContainerEntry(this.transientmortaltype(), value);

        value = "v41";
        dc.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .lifespan(100, TimeUnit.MINUTES).maxIdle(100, TimeUnit.MINUTES).build());
        assertContainerEntry(this.transientmortaltype(), value);

        value = "v5";
        dc.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        assertContainerEntry(this.mortaltype(), value);
    }


    private void assertContainerEntry(
                                        Class<? extends OffHeapInternalCacheEntry> type,
                                        String expectedValue
                                    ) {
        assert dc.containsKey("k");
        OffHeapInternalCacheEntry entry = dc.get("k");
        assertEquals(type, entry.getClass());
        assertEquals(expectedValue, entry.getValue());
    }


    public void testKeySet() {
        dc.put("k1", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        dc.put("k2", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        dc.put("k3", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        dc.put("k4", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .maxIdle(100, TimeUnit.MINUTES)
                .lifespan(100, TimeUnit.MINUTES)
                .build());

        Set<String> expected = new HashSet<String>();
        expected.add("k1");
        expected.add("k2");
        expected.add("k3");
        expected.add("k4");

        for (Object o : dc.keySet()) {
            assert expected.remove(o);
        }

        assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
    }


    public void testContainerIteration() {
        dc.put("k1", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        dc.put("k2", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        dc.put("k3", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        dc.put("k4", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

        Set<String> expected = new HashSet<String>();
        expected.add("k1");
        expected.add("k2");
        expected.add("k3");
        expected.add("k4");

        for (OffHeapInternalCacheEntry ice : dc) {
            assert expected.remove(ice.getKey());
        }

        assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
    }


    public void testKeys() {
        dc.put("k1", "v1", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        dc.put("k2", "v2", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        dc.put("k3", "v3", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        dc.put("k4", "v4", new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

        Set<String> expected = new HashSet<String>();
        expected.add("k1");
        expected.add("k2");
        expected.add("k3");
        expected.add("k4");

        for (Object o : dc.keySet()) assert expected.remove(o);

        assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
    }


    public void testValues() {
        dc.put("k1", "v1", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        dc.put("k2", "v2", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        dc.put("k3", "v3", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        dc.put("k4", "v4", new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

        Set<String> expected = new HashSet<String>();
        expected.add("v1");
        expected.add("v2");
        expected.add("v3");
        expected.add("v4");

        for (Object o : dc.values()) assert expected.remove(o);

        assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
    }


    public void testEntrySet() {
        dc.put("k1", "v1", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
        dc.put("k2", "v2", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
        dc.put("k3", "v3", new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
        dc.put("k4", "v4", new OffHeapEmbeddedMetadata.OffHeapBuilder()
                .maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

        Set<OffHeapInternalCacheEntry> expected = new HashSet<OffHeapInternalCacheEntry>();
        expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(dc.get("k1")));
        expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(dc.get("k2")));
        expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(dc.get("k3")));
        expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(dc.get("k4")));

        Set<Map.Entry<Object,Object>> actual = new HashSet<Map.Entry<Object, Object>>();
        for (Map.Entry<Object, Object> o : dc.entrySet()) actual.add(o);

        assert actual.equals(expected) : "Expected to see keys " + expected + " but only saw " + actual;
    }


    public void testGetDuringKeySetLoop() {
        for (int i = 0; i < 10; i++) dc.put(i, "value", new OffHeapEmbeddedMetadata.OffHeapBuilder().build());

        int i = 0;
        for (Object key : dc.keySet()) {
            dc.peek(key); // calling get in this situations will result on corruption the iteration.
            i++;
        }

        assert i == 10 : "Expected the loop to run 10 times, only ran " + i;
    }
}