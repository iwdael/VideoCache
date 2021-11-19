package com.danzekr.videocache.headers;

import java.util.HashMap;
import java.util.Map;

/**
 * Empty {@link HeaderInjector} implementation.
 *
 * @author Lucas Nelaupe (https://github.com/lucas34).
 */
public class EmptyHeadersInjector implements HeaderInjector {

    @Override
    public Map<String, String> addHeaders(String url) {
        return new HashMap<>();
    }

    @Override
    public Boolean filter(String url, String key) {
        return false;
    }

}
