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

import com.teragrep.buf_01.buffer.lease.TrackedLease;

import java.lang.foreign.MemorySegment;

/**
 * Decoration of {@link Writeable} Invalidates a writable so TrackedLeases' position and limit is set to zero and the
 * lease is also closed, which should fill the underlying MemorySegment with zeroes.
 */
public final class WriteableInvalidation implements Writeable {

    private final Writeable writeable;

    public WriteableInvalidation(Writeable writeable) {
        this.writeable = writeable;
    }

    @Override
    public void close() {
        for (final TrackedLease<MemorySegment> lease : memorySegmentLeases()) {
            try {
                lease.position(0L);
                lease.limit(0L);
                lease.close();
            }
            catch (final Exception e) {
                throw new RuntimeException("Error occurred whilst closing lease", e);
            }
        }
        writeable.close();
    }

    @Override
    public TrackedLease<MemorySegment>[] memorySegmentLeases() {
        return writeable.memorySegmentLeases();
    }

    @Override
    public boolean hasRemaining() {
        return writeable.hasRemaining();
    }

    @Override
    public boolean isStub() {
        return writeable.isStub();
    }

}
