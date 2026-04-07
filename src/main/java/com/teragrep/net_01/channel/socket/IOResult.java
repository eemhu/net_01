package com.teragrep.net_01.channel.socket;

import com.teragrep.buf_01.buffer.lease.Lease;

import java.util.List;

public interface IOResult<T extends Lease<?>> {
    public abstract long bytes();
    public abstract List<T> leases();
}
