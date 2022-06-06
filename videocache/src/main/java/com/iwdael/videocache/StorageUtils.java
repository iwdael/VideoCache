package com.iwdael.videocache;

import android.content.Context;
import android.os.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static android.os.Environment.MEDIA_MOUNTED;

import com.iwdael.videocache.sourcestorage.Storage;

/**
 * Provides application storage paths
 * <p/>
 * See https://github.com/nostra13/Android-Universal-Image-Loader
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
public final class StorageUtils {

    private static final Logger LOG = LoggerFactory.getLogger("StorageUtils");
    private static final String INDIVIDUAL_DIR_NAME = "video-cache";

    /**
     * Returns individual application cache directory (for only video caching from Proxy). Cache directory will be
     * created on SD card <i>("/Android/data/[app_package_name]/cache/video-cache")</i> if card is mounted .
     * Else - Android defines cache directory on device's file system.
     *
     * @param context Application context
     * @return Cache {@link File directory}
     */
    public static File getIndividualCacheDirectory(Context context) {
        File cacheDir = getCacheDirectory(context, true);
        return new File(cacheDir, INDIVIDUAL_DIR_NAME);
    }

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> (if card is mounted and app has appropriate permission) or
     * on device's file system depending incoming parameters.
     *
     * @param context        Application context
     * @param preferExternal Whether prefer external location for cache
     * @return Cache {@link File directory}.<br />
     * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
     * {@link Context#getCacheDir() Context.getCacheDir()} returns null).
     */
    private static File getCacheDirectory(Context context, boolean preferExternal) {
        File appCacheDir = null;
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens
            externalStorageState = "";
        }
        if (preferExternal && MEDIA_MOUNTED.equals(externalStorageState)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
            String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache/";
            LOG.warn("Can't define system cache directory! '" + cacheDirPath + "%s' will be used.");
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }

    private static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                LOG.warn("Unable to create external cache directory");
                return null;
            }
        }
        return appCacheDir;
    }

    public static List<CachePatch> externalPatch(List<CachePatch> patches, String url, long start, long end) throws ProxyCacheException {
        return complementaryPatch(new CachePatch(url, start, end), patches);
    }


    private static List<CachePatch> complementaryPatch(CachePatch external, List<CachePatch> sub) throws ProxyCacheException {
        List<CachePatch> subset = sub
                .stream()
                .filter(patch -> (external.start <= patch.start && patch.start <= external.end) || (external.start <= patch.end && patch.end <= external.end))
                .collect(Collectors.toList());
        List<Long> keys = Stream.concat(subset.stream(), Stream.of(external))
                .flatMap((Function<CachePatch, Stream<Long>>) patch -> Stream.of(patch.start, patch.end))
                .filter(val -> external.start <= val && val <= external.end)
                .sorted(Long::compareTo)
                .distinct()
                .collect(Collectors.toList());
        List<CachePatch> complementary = new ArrayList<>();
        for (int i = 0; i < keys.size() - 1; i++) {
            float test = (keys.get(i) + keys.get(i + 1)) / 2.0f;
            boolean exits = subset
                    .stream()
                    .anyMatch(patch -> patch.start <= test && test <= patch.end);
            if (!exits)
                complementary.add(new CachePatch(external.url, keys.get(i), keys.get(i + 1)));
        }
        return complementary;
    }

    public static boolean cacheComplete(List<CachePatch> patches, long len) {
        List<CachePatch> sorted = patches.stream().sorted(Comparator.comparingLong(o -> o.start))
                .collect(Collectors.toList());
        if (sorted.isEmpty()) return false;
        if (sorted.get(0).start != 0) return false;
        if (sorted.get(sorted.size() - 1).end != len) return false;
        for (int i = 0; i < sorted.size() - 1; i++) {
            CachePatch cur = sorted.get(i);
            CachePatch next = sorted.get(i + 1);
            if (cur.end < next.start) return false;
        }
        return true;
    }
}
