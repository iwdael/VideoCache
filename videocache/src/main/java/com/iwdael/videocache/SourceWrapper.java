package com.iwdael.videocache;

import java.util.Map;

public class SourceWrapper implements Source {
    private Source source;
    private final String url;
    private final Map<String, String> headers;
    private final Config config;
    private CacheInfo info;
    private long pointer = 0;

    public SourceWrapper(String url, Map<String, String> headers, Config config) {
        this.url = url;
        this.headers = headers;
        this.config = config;
    }

    public SourceWrapper newSelf() {
        return new SourceWrapper(url, headers, config);
    }

    private void init() {
        if (source == null) {
            synchronized (SourceWrapper.class) {
                if (source == null)
                    source = config.sourceCreator.create(url, headers);
            }
        }
    }

    private void fetchInfo() {
        if (info == null) {
            synchronized (SourceWrapper.class) {
                if (info == null) info = config.storage.getInfo(url);
                if (info == null) {
                    init();
                    saveInfo();
                }
            }
        }
    }

    private void saveInfo() {
        try {
            CacheInfo si = new CacheInfo(url, source.length(), source.getMime());
            config.storage.putInfo(url, si);
            info = si;
        } catch (Exception e) {
        }
    }

    @Override
    public void open(long offset) throws ProxyCacheException {
        init();
        if (offset == pointer) return;
        saveInfo();
        source.open(offset);
        pointer = offset;
    }

    @Override
    public long length() throws ProxyCacheException {
        fetchInfo();
        return info == null ? -1 : info.length;
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        init();
        int r = source.read(buffer);
        pointer += r;
        return r;
    }

    @Override
    public void close() throws ProxyCacheException {
        if (source != null) source.close();
    }

    @Override
    public String getMime() throws ProxyCacheException {
        fetchInfo();
        return info == null ? "" : info.mime;
    }

    @Override
    public String getUrl() {
        return url;
    }

}
