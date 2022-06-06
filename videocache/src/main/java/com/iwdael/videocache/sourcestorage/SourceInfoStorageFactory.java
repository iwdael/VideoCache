package com.iwdael.videocache.sourcestorage;

import android.content.Context;

/**
 * Simple factory for {@link Storage}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class SourceInfoStorageFactory {

    public static Storage newSourceInfoStorage(Context context) {
        return new DataStorage(context);
    }

    public static Storage newEmptySourceInfoStorage() {
        return new NoStorage();
    }
}
