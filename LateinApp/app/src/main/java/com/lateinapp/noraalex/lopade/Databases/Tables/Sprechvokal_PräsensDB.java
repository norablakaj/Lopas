package com.lateinapp.noraalex.lopade.Databases.Tables;

import android.provider.BaseColumns;

public class Sprechvokal_PräsensDB {

    private Sprechvokal_PräsensDB(){}

    public static class FeedEntry implements BaseColumns {


        // Table name
        public static final String TABLE_NAME = "Sprechvokal_Praesens";

        // Table columns
        public static final String  COLUMN_TITLE = "Titel",
                                    COLUMN_INFINITV = "Infinitiv",
                                    COLUMN_1_SG = "Erste_Sg",
                                    COLUMN_2_SG = "Zweite_Sg",
                                    COLUMN_3_SG = "Dritte_Sg",
                                    COLUMN_1_PL = "Erste_Pl",
                                    COLUMN_2_PL = "Zweite_Pl",
                                    COLUMN_3_PL = "Dritte_Pl";
    }
}
