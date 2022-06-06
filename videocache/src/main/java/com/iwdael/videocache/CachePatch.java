package com.iwdael.videocache;

import java.util.Objects;

public class CachePatch {
    public boolean isLocal = false;
    public String url;
    public long start;
    public long end;

    public CachePatch(String url, long start, long end) {
        this.url = url;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachePatch that = (CachePatch) o;
        return start == that.start && end == that.end && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, start, end);
    }

    @Override
    public String toString() {
        return "{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
