package com.application.winkwink.Utilities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* Code adapted from https://developer.android.com/training/data-storage/sqlite */

public class GameRivalsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "GameRivals.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + GameRivalsContract.RivalsEntry.TABLE_NAME + " (" +
                    GameRivalsContract.RivalsEntry._ID + " INTEGER PRIMARY KEY," +
                    GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_ID + " TEXT," +
                    GameRivalsContract.RivalsEntry.COLUMN_NAME_PLAYER_SCORE + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + GameRivalsContract.RivalsEntry.TABLE_NAME;

    public GameRivalsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}