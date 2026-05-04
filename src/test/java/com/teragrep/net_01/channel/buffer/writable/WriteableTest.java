/*
 * Java Zero Copy Networking Library net_01
 * Copyright (C) 2024 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.net_01.channel.buffer.writable;

import com.teragrep.buf_01.buffer.lease.MemorySegmentLeaseStub;
import com.teragrep.buf_01.buffer.lease.TrackedLease;
import com.teragrep.buf_01.buffer.pool.LeaseMultiGet;
import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.buf_01.buffer.pool.TrackedLeaseMultiGet;
import com.teragrep.buf_01.buffer.supply.ArenaMemorySegmentLeaseSupplier;
import com.teragrep.net_01.channel.context.StringWriteable;
import com.teragrep.poj_01.pool.UnboundPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public final class WriteableTest {

    @Test
    void testWriteableHasRemaining() {
        final OpeningPool pool = new OpeningPool(
                new UnboundPool<>(new ArenaMemorySegmentLeaseSupplier(Arena.ofShared(), 128), new MemorySegmentLeaseStub())
        );

        final TrackedLease<MemorySegment>[] trackedLeases = new TrackedLeaseMultiGet(new LeaseMultiGet(pool))
                .getAsArray(32L);

        try (final Writeable w = new StringWriteable(trackedLeases)) {
            Assertions.assertEquals(trackedLeases, w.memorySegmentLeases());
            Assertions.assertEquals(1, w.memorySegmentLeases().length);
            final TrackedLease<MemorySegment> lease = trackedLeases[0];

            int i;
            for (i = 0; i < 128; i++) {
                Assertions.assertTrue(w.hasRemaining());
                Assertions.assertTrue(lease.hasNext());
                lease.next();
            }

            Assertions.assertEquals(128, i);
            Assertions.assertFalse(w.hasRemaining());
            Assertions.assertFalse(lease.hasNext());

            Assertions.assertThrows(IndexOutOfBoundsException.class, lease::next);
        }
    }

    @Test
    void testWriteableHasRemainingWithSetLimit() {
        final OpeningPool pool = new OpeningPool(
                new UnboundPool<>(new ArenaMemorySegmentLeaseSupplier(Arena.ofShared(), 128), new MemorySegmentLeaseStub())
        );

        final TrackedLease<MemorySegment>[] trackedLeases = new TrackedLeaseMultiGet(new LeaseMultiGet(pool))
                .getAsArray(32L);

        try (final Writeable w = new StringWriteable(trackedLeases)) {
            Assertions.assertEquals(trackedLeases, w.memorySegmentLeases());
            Assertions.assertEquals(1, w.memorySegmentLeases().length);
            final TrackedLease<MemorySegment> lease = trackedLeases[0];
            lease.limit(32L);

            int i;
            for (i = 0; i < 32; i++) {
                Assertions.assertTrue(w.hasRemaining());
                Assertions.assertTrue(lease.hasNext());
                lease.next();
            }

            Assertions.assertEquals(32, i);
            Assertions.assertFalse(w.hasRemaining());
            Assertions.assertFalse(lease.hasNext());

            Assertions.assertThrows(IndexOutOfBoundsException.class, lease::next);
        }
    }
}
