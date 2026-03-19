package com.teragrep.net_01.channel.context.buffer;

import com.teragrep.buf_01.buffer.lease.MemorySegmentLeaseStub;
import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.buf_01.buffer.supply.ArenaMemorySegmentLeaseSupplier;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.poj_01.pool.UnboundPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

public final class TrackedMemorySegmentLeaseTest
{
    @Test
    void testProgressing() {
        final OpeningPool pool = new OpeningPool(new UnboundPool<>(new ArenaMemorySegmentLeaseSupplier(Arena.ofShared(), 5), new MemorySegmentLeaseStub()));
        final TrackedMemorySegmentLease trackedLease = new TrackedMemorySegmentLease(pool.get());

        Assertions.assertEquals(0L, trackedLease.position());

        int loops = 0;
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(i, trackedLease.position());
            Assertions.assertTrue(trackedLease.hasNext());
            Assertions.assertEquals((byte)0, trackedLease.next());
            loops++;
        }
        Assertions.assertEquals(5, loops);

        Assertions.assertFalse(trackedLease.hasNext());
        Assertions.assertThrows(IndexOutOfBoundsException.class, trackedLease::next);
        Assertions.assertEquals(5L, trackedLease.position());
    }
}

