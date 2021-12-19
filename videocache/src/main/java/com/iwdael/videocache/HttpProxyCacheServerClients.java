package com.iwdael.videocache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.iwdael.videocache.file.FileCache;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.iwdael.videocache.Preconditions.checkNotNull;

/**
 * Client for {@link HttpProxyCacheServer}
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
final class HttpProxyCacheServerClients {

    private final AtomicInteger clientsCount = new AtomicInteger(0);
    private final String url;
    private volatile HttpProxyCache proxyCache;
    private final List<CacheListener> listeners = new CopyOnWriteArrayList<>();
    private final CacheListener uiCacheListener;
    private final Config config;

    public HttpProxyCacheServerClients(String url, Config config) {
        this.url = checkNotNull(url);
        this.config = checkNotNull(config);
        this.uiCacheListener = new UiListenerHandler(url, listeners);
    }

    public void processRequest(GetRequest request, Socket socket) throws ProxyCacheException, IOException {
        startProcessRequest(request);
        try {
            clientsCount.incrementAndGet();
            proxyCache.processRequest(request, socket);
        } finally {
            finishProcessRequest();
        }
    }

    private synchronized void startProcessRequest(GetRequest request) throws ProxyCacheException {
        proxyCache = proxyCache == null ? newHttpProxyCache(request) : proxyCache;
    }

    private synchronized void finishProcessRequest() {
        if (clientsCount.decrementAndGet() <= 0) {
            proxyCache.shutdown();
            proxyCache = null;
        }
    }

    public void registerCacheListener(CacheListener cacheListener) {
        listeners.add(cacheListener);
    }

    public void unregisterCacheListener(CacheListener cacheListener) {
        listeners.remove(cacheListener);
    }

    public void shutdown() {
        listeners.clear();
        if (proxyCache != null) {
            proxyCache.registerCacheListener(null);
            proxyCache.shutdown();
            proxyCache = null;
        }
        clientsCount.set(0);
    }

    public int getClientsCount() {
        return clientsCount.get();
    }

    private HttpProxyCache newHttpProxyCache(GetRequest request) throws ProxyCacheException {
        Map<String, String> headers = newHttpConnectionHeader(request);
        Source source  = config.sourceCreator.create(url, config.sourceInfoStorage, headers);
        FileCache cache = new FileCache(config.generateCacheFile(url), config.diskUsage);
        HttpProxyCache httpProxyCache = new HttpProxyCache(source, cache,config);
        httpProxyCache.registerCacheListener(uiCacheListener);
        return httpProxyCache;
    }

    private Map<String, String> newHttpConnectionHeader(GetRequest request) {
        Map<String, String> headers = new HashMap<>(config.headerInjector.addHeaders(url));
        for (Map.Entry<String, String> entry : request.headers.entrySet()) {
            if (config.headerInjector.filter(url, entry.getKey()))
                headers.put(entry.getKey(), entry.getValue());
        }
        return headers;
    }

    private static final class UiListenerHandler extends Handler implements CacheListener {

        private final String url;
        private final List<CacheListener> listeners;

        public UiListenerHandler(String url, List<CacheListener> listeners) {
            super(Looper.getMainLooper());
            this.url = url;
            this.listeners = listeners;
        }

        @Override
        public void onCacheAvailable(File file, String url, int percentsAvailable) {
            Message message = obtainMessage();
            message.arg1 = percentsAvailable;
            message.obj = file;
            sendMessage(message);
        }

        @Override
        public void handleMessage(Message msg) {
            for (CacheListener cacheListener : listeners) {
                cacheListener.onCacheAvailable((File) msg.obj, url, msg.arg1);
            }
        }
    }
}
