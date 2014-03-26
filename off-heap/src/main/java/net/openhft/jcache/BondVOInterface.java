package net.openhft.jcache;

import net.openhft.lang.io.Bytes;
import net.openhft.lang.model.constraints.MaxSize;
//import org.infinispan.offheap.container.entries.OffHeapInternalCacheEntry;


public interface BondVOInterface {

    /* add support for entry based locking */
    void busyLockEntry() throws InterruptedException;
    void unlockEntry();

	void setRecord(Bytes record);
    Bytes getRecord();

    void setEntry(Bytes entry);
    Bytes getEntry();

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

    //OpenHFT Off-Heap array[ ] processing notice ‘At’ suffix
    void setMarketPxIntraDayHistoryAt(@MaxSize(7) int tradingDayHour, MarketPx mPx);
    MarketPx getMarketPxIntraDayHistoryAt(int tradingDayHour);

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

