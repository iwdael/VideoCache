package com.iwdael.videocache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Simple memory based {@link Cache} implementation.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class ByteArrayCache implements Cache {

    private volatile byte[] data;
    private volatile boolean completed;

    public ByteArrayCache() {
        this(new byte[0]);
    }

    public ByteArrayCache(byte[] data) {
        this.data = Preconditions.checkNotNull(data);
    }

    @Override
    public int read(byte[] buffer, long pointer, int offset, int length) throws ProxyCacheException {
        if (pointer >= data.length) {
            return -1;
        }
        if (pointer > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too long offset for memory cache " + pointer);
        }
        return new ByteArrayInputStream(data).read(buffer, (int) pointer, length);
    }


    @Override
    public String key() {
        return null;
    }

    @Override
    public void write(byte[] newData, long pointer, int offset, int length) throws ProxyCacheException {
        Preconditions.checkNotNull(data);
        Preconditions.checkArgument(length >= 0 && length <= newData.length);

        byte[] appendedData = Arrays.copyOf(data, data.length + length);
        System.arraycopy(newData, 0, appendedData, data.length, length);
        data = appendedData;
    }

    @Override
    public void close() throws ProxyCacheException {
    }

    @Override
    public void complete() {
        completed = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }


    @Override
    public void putPatch(long start, long end) {

    }

    @Override
    public List<CachePatch> externalPatch(long start, long end) {
        return null;
    }

    @Override
    public boolean readyPatch(long len) {
        return true;
    }

    @Override
    public File getFile() {
        return null;
    }
}
