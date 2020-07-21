package com.application.winkwink.Utilities;

import android.provider.BaseColumns;

public final class GameRivalsContract {

    private GameRivalsContract() {
    }

    /* Inner class that defines the table contents */
    public static class RivalsEntry implements BaseColumns {
        public static final String TABLE_NAME = "rivals";
        public static final String COLUMN_NAME_PLAYER_ID = "identifier";
        public static final String COLUMN_NAME_PLAYER_SCORE = "score";
    }
}