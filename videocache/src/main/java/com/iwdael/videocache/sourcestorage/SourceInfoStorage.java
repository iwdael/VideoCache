package com.iwdael.videocache.sourcestorage;

import com.iwdael.videocache.SourceInfo;

/**
 * Storage for {@link SourceInfo}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface SourceInfoStorage {

    SourceInfo get(String url);

    void put(String url, SourceInfo sourceInfo);

    void release();
}
