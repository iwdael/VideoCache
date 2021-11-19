package com.danzekr.videocache;

import com.danzekr.videocache.file.DiskUsage;
import com.danzekr.videocache.file.FileNameGenerator;
import com.danzekr.videocache.headers.HeaderInjector;
import com.danzekr.videocache.sourcestorage.SourceInfoStorage;

import java.io.File;

/**
 * Configuration for proxy cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class Config {

    public final File cacheRoot;
    public final FileNameGenerator fileNameGenerator;
    public final DiskUsage diskUsage;
    public final SourceInfoStorage sourceInfoStorage;
    public final HeaderInjector headerInjector;
    public final SourceCreator sourceCreator;

    Config(File cacheRoot, FileNameGenerator fileNameGenerator, DiskUsage diskUsage, SourceInfoStorage sourceInfoStorage, HeaderInjector headerInjector, SourceCreator sourceCreator) {
        this.cacheRoot = cacheRoot;
        this.fileNameGenerator = fileNameGenerator;
        this.diskUsage = diskUsage;
        this.sourceInfoStorage = sourceInfoStorage;
        this.headerInjector = headerInjector;
        this.sourceCreator = sourceCreator;
    }

    File generateCacheFile(String url) {
        String name = fileNameGenerator.generate(url);
        return new File(cacheRoot, name);
    }

}
