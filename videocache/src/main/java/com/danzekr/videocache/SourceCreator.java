package com.danzekr.videocache;

import com.danzekr.videocache.sourcestorage.SourceInfoStorage;

import java.util.Map;

public interface SourceCreator<T extends Source> {
    T create(String url, SourceInfoStorage sourceInfoStorage, Map<String, String> headers);

    T create(T source);
}
