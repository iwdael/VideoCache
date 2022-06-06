package com.iwdael.videocache.sourcestorage;

import com.iwdael.videocache.CacheInfo;
import com.iwdael.videocache.CachePatch;

import java.util.List;

/**
 * Storage for {@link CacheInfo}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Storage {


    CacheInfo getInfo(String url);

    void putInfo(String url, CacheInfo cacheInfo);

    List<CachePatch> getPatch(String url);

    void putPatch(String url, CachePatch patch);

    void clearPatch(String url);

    void release();

}
