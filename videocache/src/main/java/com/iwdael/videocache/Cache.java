package com.iwdael.videocache;

import java.io.File;
import java.util.List;

/**
 * Cache for proxy.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Cache {

    String key();

    int read(byte[] buffer, long pointer, int offset, int length) throws ProxyCacheException;

    void write(byte[] data, long pointer, int offset, int length) throws ProxyCacheException;

    void close() throws ProxyCacheException;

    void complete() throws ProxyCacheException;

    boolean isCompleted();

    void putPatch(long start, long end);

    List<CachePatch> externalPatch(long start, long end);

    boolean readyPatch(long len);

    File getFile();
}
