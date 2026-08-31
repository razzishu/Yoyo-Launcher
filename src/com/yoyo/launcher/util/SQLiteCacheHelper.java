package com.yoyo.launcher.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteCacheHelper {
    public SQLiteCacheHelper(Context context, String dbName, int version, String tableName) {}
    public void close() {}
    
    public Cursor query(String[] columns, String selection, String[] selectionArgs) {
        return new MatrixCursor(columns != null ? columns : new String[0]);
    }
    
    public void insertOrReplace(ContentValues values) {}
    public void delete(String whereClause, String[] whereArgs) {}
    
    public static SQLiteDatabase.OpenParams createNoLocaleParams() {
        return new SQLiteDatabase.OpenParams.Builder()
                .addOpenFlags(SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                .build();
    }
}
