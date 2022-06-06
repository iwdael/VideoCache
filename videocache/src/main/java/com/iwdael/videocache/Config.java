package com.iwdael.videocache;

import com.iwdael.videocache.file.DiskUsage;
import com.iwdael.videocache.file.FileNameGenerator;
import com.iwdael.videocache.headers.HeaderInjector;
import com.iwdael.videocache.sourcestorage.Storage;

import java.io.File;

/**
 * Configuration for proxy cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class Config {

    public final File cacheRoot;
    public final FileNameGenerator fileNameGenerator;
    public final DiskUsage diskUsage;
    public final Storage storage;
    public final HeaderInjector headerInjector;
    public final SourceCreator<Source> sourceCreator;

    Config(File cacheRoot, FileNameGenerator fileNameGenerator, DiskUsage diskUsage, Storage storage, HeaderInjector headerInjector, SourceCreator<Source> sourceCreator) {
        this.cacheRoot = cacheRoot;
        this.fileNameGenerator = fileNameGenerator;
        this.diskUsage = diskUsage;
        this.storage = storage;
        this.headerInjector = headerInjector;
        this.sourceCreator = sourceCreator;
    }

    File generateCacheFile(String url) {
        String name = fileNameGenerator.generate(url);
        return new File(cacheRoot, name);
    }

}
