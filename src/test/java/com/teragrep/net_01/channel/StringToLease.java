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
package com.teragrep.net_01.channel;

import com.teragrep.buf_01.buffer.lease.OpenableLease;
import com.teragrep.buf_01.buffer.pool.LeaseMultiGet;
import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.net_01.channel.buffer.writable.Writeable;
import com.teragrep.net_01.channel.context.StringWriteable;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class StringToLease {

    private final String origin;
    private final OpeningPool pool;

    public StringToLease(final String origin, OpeningPool pool) {
        this.origin = origin;
        this.pool = pool;
    }

    public Writeable toWriteable() {
        final byte[] bytes = origin.getBytes(StandardCharsets.UTF_8);
        final List<OpenableLease<MemorySegment>> leases = new LeaseMultiGet(pool).get(bytes.length);
        final List<TrackedMemorySegmentLease> trackedLeases = new ArrayList<>(leases.size());
        for (final OpenableLease<MemorySegment> lease : leases) {
            trackedLeases.add(new TrackedMemorySegmentLease(lease));
        }

        int i = 0;
        for (final TrackedMemorySegmentLease trackedLease : trackedLeases) {
            while (trackedLease.hasNext() && i < bytes.length) {
                trackedLease.write(bytes[i]);
                i++;
            }
        }

        return new StringWriteable(trackedLeases);
    }
}
