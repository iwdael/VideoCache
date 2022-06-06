package com.iwdael.videocache.file;

import com.iwdael.videocache.Cache;
import com.iwdael.videocache.CachePatch;
import com.iwdael.videocache.Config;
import com.iwdael.videocache.ProxyCacheException;
import com.iwdael.videocache.StorageUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Cache} that uses file for storing data.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class FileCache implements Cache {
    public static final String TAG = "cache";
    private static final String TEMP_POSTFIX = ".download";

    private final DiskUsage diskUsage;
    private final Config config;
    private final String key;
    private final List<CachePatch> patches;
    public File file;
    private RandomAccessFile dataFile;


    public FileCache(File file, Config config, String key) throws ProxyCacheException {
        try {
            if (config == null) {
                throw new NullPointerException();
            }
            this.key = key;
            this.config = config;
            this.diskUsage = config.diskUsage;
            File directory = file.getParentFile();
            Files.makeDir(directory);
            boolean completed = file.exists();
            this.file = completed ? file : new File(file.getParentFile(), file.getName() + TEMP_POSTFIX);
            if (!this.file.exists()) {
                this.patches = new ArrayList<>();
                config.storage.clearPatch(key);
            } else {
                this.patches = config.storage.getPatch(key);
                this.patches.forEach(patch -> patch.isLocal = true);
            }
            this.dataFile = new RandomAccessFile(this.file, completed ? "r" : "rw");
        } catch (IOException e) {
            throw new ProxyCacheException("Error using file " + file + " as disc cache", e);
        }
    }


    @Override
    public String key() {
        return key;
    }

    @Override
    public synchronized int read(byte[] buffer, long pointer, int offset, int length) throws ProxyCacheException {
        try {
            dataFile.seek(pointer);
            return dataFile.read(buffer, offset, length);
        } catch (IOException e) {
            String format = "Error reading %d bytes with offset %d from file[%d bytes] to buffer[%d bytes]";
            throw new ProxyCacheException(String.format(format, length, offset, pointer, buffer.length), e);
        }
    }

    @Override
    public synchronized void write(byte[] data, long pointer, int offset, int length) throws ProxyCacheException {
        try {
            if (isCompleted()) {
                throw new ProxyCacheException("Error append cache: cache file " + file + " is completed!");
            }
            dataFile.seek(pointer);
            dataFile.write(data, offset, length);
        } catch (IOException e) {
            e.printStackTrace();
            String format = "Error writing %d bytes to %s from buffer with size %d";
            throw new ProxyCacheException(String.format(format, length, dataFile, data.length), e);
        }
    }

    @Override
    public synchronized void close() throws ProxyCacheException {
        try {
            if (!patches.isEmpty()) {
                CachePatch last = patches.get(patches.size() - 1);
                if (!last.isLocal) {
                    config.storage.putPatch(key, last);
                    last.isLocal = true;
                }
            }
            dataFile.close();
            diskUsage.touch(file);
        } catch (IOException e) {
            throw new ProxyCacheException("Error closing file " + file, e);
        }
    }

    @Override
    public synchronized void complete() throws ProxyCacheException {
        if (isCompleted()) {
            return;
        }

        close();
        String fileName = file.getName().substring(0, file.getName().length() - TEMP_POSTFIX.length());
        File completedFile = new File(file.getParentFile(), fileName);
        boolean renamed = file.renameTo(completedFile);
        if (!renamed) {
            throw new ProxyCacheException("Error renaming file " + file + " to " + completedFile + " for completion!");
        }
        file = completedFile;
        try {
            dataFile = new RandomAccessFile(file, "r");
            diskUsage.touch(file);
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening " + file + " as disc cache", e);
        }
    }

    @Override
    public synchronized boolean isCompleted() {
        return !isTempFile(file);
    }


    @Override
    public synchronized void putPatch(long start, long end) {
        if (patches.isEmpty()) {
            patches.add(new CachePatch(key, start, end));
        } else {
            CachePatch last = patches.get(patches.size() - 1);
            if (last.end == start) {
                last.end = end;
            } else {
                if (!last.isLocal) {
                    config.storage.putPatch(key, last);
                    last.isLocal = true;
                }
                patches.add(new CachePatch(key, start, end));
            }
        }
    }

    @Override
    public synchronized List<CachePatch> externalPatch(long start, long end) {
        try {
            return StorageUtils.externalPatch(patches, key, start, end);
        } catch (ProxyCacheException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Returns file to be used fo caching. It may as original file passed in constructor as some temp file for not completed cache.
     *
     * @return file for caching.
     */
    @Override
    public File getFile() {
        return file;
    }

    private boolean isTempFile(File file) {
        return file.getName().endsWith(TEMP_POSTFIX);
    }

    @Override
    public synchronized boolean readyPatch(long len) {
        return StorageUtils.cacheComplete(patches, len);
    }
}
