package com.iwdael.videocache;

import com.iwdael.videocache.sourcestorage.SourceInfoStorage;

import java.util.Map;

public class DefaultSourceCreator implements SourceCreator<HttpUrlSource> {
    @Override
    public HttpUrlSource create(String url, SourceInfoStorage sourceInfoStorage, Map<String, String> headers) {
        return new HttpUrlSource(url, sourceInfoStorage, headers);
    }

    @Override
    public HttpUrlSource create(HttpUrlSource source) {
        return new HttpUrlSource(source);
    }
}
