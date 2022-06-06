package com.iwdael.videocache;

import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.iwdael.videocache.Preconditions.checkNotNull;
import static com.iwdael.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

/**
 * {@link Source} that uses http resource as source for {@link ProxyCache}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpUrlSource implements Source {

    private static final Logger LOG = LoggerFactory.getLogger("HttpUrlSource");

    private static final int MAX_REDIRECTS = 5;

    private final Map<String, String> headers;
    private CacheInfo cacheInfo;
    private HttpURLConnection connection;
    private InputStream inputStream;


    public HttpUrlSource(String url) {
        this(url, new HashMap<String, String>());
    }

    public HttpUrlSource(String url, Map<String, String> headers) {
        this.cacheInfo = cacheInfo != null ? cacheInfo :
                new CacheInfo(url, Integer.MIN_VALUE, ProxyCacheUtils.getSupposablyMime(url));
        this.headers = headers;

    }



    @Override
    public synchronized long length() throws ProxyCacheException {
        if (cacheInfo.length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return cacheInfo.length;
    }

    @Override
    public void open(long offset) throws ProxyCacheException {
        try {
            connection = openConnection(offset, -1);
            String mime = connection.getContentType();
            inputStream = new BufferedInputStream(connection.getInputStream(), DEFAULT_BUFFER_SIZE);
            long length = readSourceAvailableBytes(connection, offset, connection.getResponseCode());
            this.cacheInfo = new CacheInfo(cacheInfo.url, length, mime);
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening connection for " + cacheInfo.url + " with offset " + offset, e);
        }
    }

    private long readSourceAvailableBytes(HttpURLConnection connection, long offset, int responseCode) throws IOException {
        long contentLength = getContentLength(connection);
        return responseCode == HTTP_OK ? contentLength
                : responseCode == HTTP_PARTIAL ? contentLength + offset : cacheInfo.length;
    }

    private long getContentLength(HttpURLConnection connection) {
        String contentLengthValue = connection.getHeaderField("Content-Length");
        return contentLengthValue == null ? -1 : Long.parseLong(contentLengthValue);
    }

    @Override
    public void close() throws ProxyCacheException {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (NullPointerException | IllegalArgumentException e) {
                String message = "Wait... but why? WTF!? " +
                        "Really shouldn't happen any more after fixing https://github.com/danikula/AndroidVideoCache/issues/43. " +
                        "If you read it on your device log, please, notify me danikula@gmail.com or create issue here " +
                        "https://github.com/danikula/AndroidVideoCache/issues.";
                throw new RuntimeException(message, e);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOG.error("Error closing connection correctly. Should happen only on Android L. " +
                        "If anybody know how to fix it, please visit https://github.com/danikula/AndroidVideoCache/issues/88. " +
                        "Until good solution is not know, just ignore this issue :(", e);
            }
        }
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        if (inputStream == null) {
            throw new ProxyCacheException("Error reading data from " + cacheInfo.url + ": connection is absent!");
        }
        try {
            return inputStream.read(buffer, 0, buffer.length);
        } catch (InterruptedIOException e) {
            throw new InterruptedProxyCacheException("Reading source " + cacheInfo.url + " is interrupted", e);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + cacheInfo.url, e);
        }
    }

    private void fetchContentInfo() throws ProxyCacheException {
        LOG.debug("Read content info from " + cacheInfo.url);
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = openConnection(0, 10000);
            long length = getContentLength(urlConnection);
            String mime = urlConnection.getContentType();
            inputStream = urlConnection.getInputStream();
            this.cacheInfo = new CacheInfo(cacheInfo.url, length, mime);
            LOG.debug("Source info fetched: " + cacheInfo);
        } catch (IOException e) {
            LOG.error("Error fetching info from " + cacheInfo.url, e);
        } finally {
            ProxyCacheUtils.close(inputStream);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(long offset, int timeout) throws IOException, ProxyCacheException {
        HttpURLConnection connection;
        boolean redirected;
        int redirectCount = 0;
        String url = this.cacheInfo.url;
        do {
            LOG.debug("Open connection " + (offset > 0 ? " with offset " + offset : "") + " to " + url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            injectCustomHeaders(connection);
            if (offset > 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            if (timeout > 0) {
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
            }
            int code = connection.getResponseCode();
            redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER;
            if (redirected) {
                url = connection.getHeaderField("Location");
                redirectCount++;
                connection.disconnect();
            }
            if (redirectCount > MAX_REDIRECTS) {
                throw new ProxyCacheException("Too many redirects: " + redirectCount);
            }
        } while (redirected);
        return connection;
    }

    private void injectCustomHeaders(HttpURLConnection connection) {
        Map<String, String> extraHeaders = headers;
        for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
    }

    public synchronized String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(cacheInfo.mime)) {
            fetchContentInfo();
        }
        return cacheInfo.mime;
    }

    @Override
    public String getUrl() {
        return cacheInfo.url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{sourceInfo='" + cacheInfo + "}";
    }
}
