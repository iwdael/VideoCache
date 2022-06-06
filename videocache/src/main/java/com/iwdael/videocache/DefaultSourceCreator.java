package com.iwdael.videocache;

import java.util.Map;

public class DefaultSourceCreator implements SourceCreator<Source> {
    @Override
    public Source create(String url, Map<String, String> headers) {
        return new HttpUrlSource(url, headers);
    }


}
