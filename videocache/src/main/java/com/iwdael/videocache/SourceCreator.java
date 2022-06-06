package com.iwdael.videocache;

import java.util.Map;

public interface SourceCreator<T extends Source> {
    T create(String url, Map<String, String> headers);

}
