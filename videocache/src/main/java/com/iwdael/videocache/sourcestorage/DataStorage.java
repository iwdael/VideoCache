package com.iwdael.videocache.sourcestorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.iwdael.videocache.CacheInfo;
import com.iwdael.videocache.CachePatch;

import static com.iwdael.videocache.Preconditions.checkAllNotNull;
import static com.iwdael.videocache.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Database based {@link Storage}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class DataStorage extends SQLiteOpenHelper implements Storage {

    private static final String TABLE_INFO = "CACHE_INFO";
    private static final String TABLE_PATCH = "CACHE_PATCH";
    private static final String COLUMN_ID = "COL_ID";
    private static final String COLUMN_URL = "COL_URL";
    private static final String COLUMN_LENGTH = "COL_LENGTH";
    private static final String COLUMN_MIME = "COL_MIME";
    private static final String COLUMN_START = "COL_START";
    private static final String COLUMN_END = "COL_END";
    private static final String[] ALL_INFO_COLUMNS = new String[]{COLUMN_ID, COLUMN_URL, COLUMN_LENGTH, COLUMN_MIME};
    private static final String[] ALL_PATCH_COLUMNS = new String[]{COLUMN_ID, COLUMN_URL, COLUMN_START, COLUMN_END};
    private static final String CREATE_INFO_SQL =
            "CREATE TABLE " + TABLE_INFO + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    COLUMN_URL + " TEXT NOT NULL," +
                    COLUMN_MIME + " TEXT," +
                    COLUMN_LENGTH + " INTEGER" +
                    ");";

    private static final String CREATE_PATCH_SQL =
            "CREATE TABLE " + TABLE_PATCH + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    COLUMN_URL + " TEXT NOT NULL," +
                    COLUMN_START + " INTEGER," +
                    COLUMN_END + " INTEGER" +
                    ");";

    DataStorage(Context context) {
        super(context, "ProxyCache.db", null, 1);
        checkNotNull(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        checkNotNull(db);
        db.execSQL(CREATE_INFO_SQL);
        db.execSQL(CREATE_PATCH_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Should not be called. There is no any migration");
    }

    @Override
    public CacheInfo getInfo(String url) {
        checkNotNull(url);
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_INFO, ALL_INFO_COLUMNS, COLUMN_URL + "=?", new String[]{url}, null, null, null);
            return cursor == null || !cursor.moveToFirst() ? null : convertSource(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public List<CachePatch> getPatch(String url) {
        checkNotNull(url);
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_PATCH, ALL_PATCH_COLUMNS, COLUMN_URL + "=?", new String[]{url}, null, null, null);
            List<CachePatch> patches = new ArrayList<>();
            if (cursor == null) return patches;
            while (cursor.moveToNext()) {
                patches.add(convertPatch(cursor));
            }
            return patches;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void putInfo(String url, CacheInfo cacheInfo) {
        checkAllNotNull(url, cacheInfo);
        CacheInfo cacheInfoFromDb = getInfo(url);
        boolean exist = cacheInfoFromDb != null;
        ContentValues contentValues = convertSource(cacheInfo);
        if (exist) {
            getWritableDatabase().update(TABLE_INFO, contentValues, COLUMN_URL + "=?", new String[]{url});
        } else {
            getWritableDatabase().insert(TABLE_INFO, null, contentValues);
        }
    }

    @Override
    public void putPatch(String url, CachePatch patch) {
        checkAllNotNull(url, patch);
        ContentValues contentValues = convertPatch(patch);
        getWritableDatabase().insert(TABLE_PATCH, null, contentValues);

    }

    @Override
    public void clearPatch(String url) {
        getWritableDatabase().delete(TABLE_PATCH, COLUMN_URL + "=?", new String[]{url});
    }

    @Override
    public void release() {
        close();
    }

    private CachePatch convertPatch(Cursor cursor) {
        return new CachePatch(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END))
        );
    }

    private CacheInfo convertSource(Cursor cursor) {
        return new CacheInfo(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LENGTH)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIME))
        );
    }

    private ContentValues convertSource(CacheInfo cacheInfo) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, cacheInfo.url);
        values.put(COLUMN_LENGTH, cacheInfo.length);
        values.put(COLUMN_MIME, cacheInfo.mime);
        return values;
    }


    private ContentValues convertPatch(CachePatch patch) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, patch.url);
        values.put(COLUMN_START, patch.start);
        values.put(COLUMN_END, patch.end);
        return values;
    }
}
