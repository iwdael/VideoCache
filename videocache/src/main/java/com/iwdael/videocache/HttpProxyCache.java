package com.iwdael.videocache;

import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Locale;

import static com.iwdael.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * {@link ProxyCache} that read http url and writes data to {@link Socket}
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class HttpProxyCache extends ProxyCache {

    private static final float NO_CACHE_BARRIER = .2f;

    private CacheListener listener;

    private final Config config;

    public HttpProxyCache(Source source, Cache cache, Config config) {
        super(source, cache);
        this.config = config;
    }

    public void registerCacheListener(CacheListener cacheListener) {
        this.listener = cacheListener;
    }

    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        String responseHeaders = newResponseHeaders(request);
        out.write(responseHeaders.getBytes("UTF-8"));
        long offset = request.rangeOffset;
        if (isUseCache(request)) {
            responseWithCache(out, offset);
        } else {
            responseWithoutCache(out, offset);
        }
    }

    private boolean isUseCache(GetRequest request) throws ProxyCacheException {
        long sourceLength = source.length();
        boolean sourceLengthKnown = sourceLength > 0;
        // do not use cache for partial requests which too far from available cache. It seems user seek video.
        return !sourceLengthKnown || !request.partial;
    }

    private String newResponseHeaders(GetRequest request) throws IOException, ProxyCacheException {
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        long length = source.length();
        boolean lengthKnown = length >= 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(mimeKnown ? format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    private void responseWithCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long pointer = offset;
        int r;
        while ((r = read(buffer, pointer, buffer.length)) != -1) {
            out.write(buffer, 0, r);
            pointer += r;
            out.flush();
        }
    }

    private void responseWithoutCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        SourceWrapper sourceWrapper = (SourceWrapper) this.source;
        Source newSourceNoCache = sourceWrapper.newSelf();
        try {
            newSourceNoCache.open((int) offset);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                offset += readBytes;
            }
            out.flush();
        } finally {
            newSourceNoCache.close();
        }
    }

    private String format(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }

    @Override
    protected void onCachePercentsAvailableChanged(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.getFile(), source.getUrl(), percents);
        }
    }
}
