package org.infinispan.offheap.container;

import org.infinispan.context.Flag;
import org.infinispan.metadata.Metadata;

/**
 * Created by ben.cotton@jpmorgan.com on 3/18/14.
 */
public class OffHeapFlagAffectedCommand {
    public Metadata GetOffHeapMetadata() {
        return null;
    }

    public boolean hasFlag(Flag putForExternalRead) {
        return false;
    }
}
