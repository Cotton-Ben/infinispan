package org.infinispan.offheap;

import net.openhft.lang.model.constraints.MaxSize;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.metadata.Metadata;

/**
 *
 */
interface BondVOInterface extends InternalCacheEntry {

    /* add support for entry based locking */
    void busyLockEntry() throws InterruptedException;
    void unlockEntry();

    long getIssueDate();
    void setIssueDate(long issueDate);  /* time in millis */

    long getMaturityDate();
    void setMaturityDate(long maturityDate);  /* time in millis */

    long addAtomicMaturityDate(long toAdd);

    double getCoupon();
    void setCoupon(double coupon);

    double addAtomicCoupon(double toAdd);

    void setSymbol(@MaxSize(20) String symbol);
    String getSymbol();

    // OpenHFT Off-Heap array[ ] processing notice ‘At’ suffix
    void setMarketPxIntraDayHistoryAt(@MaxSize(7) int tradingDayHour, MarketPx mPx);
    MarketPx getMarketPxIntraDayHistoryAt(int tradingDayHour);

    @Override
    boolean isExpired(long now);

    @Override
    boolean isExpired();

    @Override
    boolean canExpire();

    @Override
    long getCreated();

    @Override
    long getLastUsed();

    @Override
    long getExpiryTime();

    @Override
    void touch();

    @Override
    void touch(long currentTimeMillis);

    @Override
    void reincarnate();

    @Override
    void reincarnate(long now);

    @Override
    InternalCacheValue toInternalCacheValue();

    @Override
    InternalCacheEntry clone();

    @Override
    boolean isNull();

    @Override
    boolean isChanged();

    @Override
    boolean isCreated();

    @Override
    boolean isRemoved();

    @Override
    boolean isEvicted();

    @Override
    boolean isValid();

    @Override
    boolean isLoaded();

    @Override
    Object getKey();

    @Override
    Object getValue();

    @Override
    long getLifespan();

    @Override
    long getMaxIdle();

    @Override
    boolean skipLookup();

    @Override
    Object setValue(Object value);

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    void commit(DataContainer container, Metadata metadata);

    @Override
    void rollback();

    @Override
    void setChanged(boolean changed);

    @Override
    void setCreated(boolean created);

    @Override
    void setRemoved(boolean removed);

    @Override
    void setEvicted(boolean evicted);

    @Override
    void setValid(boolean valid);

    @Override
    void setLoaded(boolean loaded);

    @Override
    void setSkipLookup(boolean skipLookup);

    @Override
    boolean undelete(boolean doUndelete);

    @Override
    Metadata getMetadata();

    @Override
    void setMetadata(Metadata metadata);

    /* nested interface - empowering an Off-Heap hierarchical “TIER of prices”
    as array[ ] value */
    interface MarketPx {
        double getCallPx();
        void setCallPx(double px);

        double getParPx();
        void setParPx(double px);

        double getMaturityPx();
        void setMaturityPx(double px);

        double getBidPx();
        void setBidPx(double px);

        double getAskPx();
        void setAskPx(double px);
    }
}

