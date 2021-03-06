package com.lateinapp.noraalex.lopade.Databases;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.lateinapp.noraalex.lopade.Databases.Tables.AdjektivDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.AdverbDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.BeispielsatzDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.DeklinationsendungDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.LektionDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.Personalendung_PräsensDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.PräpositionDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.Sprechvokal_PräsensDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.Sprechvokal_SubstantivDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.SprichwortDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.SubjunktionDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.SubstantivDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.VerbDB;
import com.lateinapp.noraalex.lopade.Databases.Tables.Vokabel;
import com.lateinapp.noraalex.lopade.General;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

import static com.lateinapp.noraalex.lopade.Databases.SQL_DUMP.*;
import static com.lateinapp.noraalex.lopade.Global.KEY_NOT_FIRST_STARTUP;

/**
 * DBHelper is used for managing the database and its tables.
 * All queries are done in this class and called where they are needed
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    private static DBHelper sInstance;

    //Version of the database. Currently of no use.
    private static final int DATABASE_VERSION = 1;

    //private final Context context;

    //Name of the database file on the target device.
    private static final String DATABASE_NAME = "Database.db";

    public static void firstStartup(Context context){
        SharedPreferences sharedPreferences = General.getSharedPrefrences(context);
        copyDataBaseFromAssets(context);
        //fillDatabaseFromCsv(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOT_FIRST_STARTUP, true);
        editor.apply();
    }

    /**
     * Used for the initialisation of the database: similar to a setup()/init() method.
     *
     * Adding initial entries to the database.
     *
     * @param context Application Context
     */
    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creating all tables
     *
     * @param db target database
     */
    public void onCreate(SQLiteDatabase db) {

        createTables(db);
    }

    /**
     * This is currently unused and not implemented properly.
     * Currently just deletes all tables and recreates them.
     *
     * @param db         Database that should be upgraded
     * @param oldVersion old versionNr of the database
     * @param newVersion new versionNr of the database
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //TODO: This should also add the initial entries. -> we need to get context here for that
        deleteEntries(db);

        onCreate(db);
    }

    public static synchronized DBHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DBHelper(context.getApplicationContext());
        }
        return sInstance;
    }


    //
    // Methods for initializing the database
    //

    public static void copyDataBaseFromAssets(Context context) {

        try {
            InputStream input = context.getAssets().open(DATABASE_NAME);
            String outFileName = context.getDatabasePath(DATABASE_NAME).toString();

            OutputStream output = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
            output.close();
            input.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void fillDatabaseFromCsv(Context context){

        //Clearing the database if it is still full
        SQLiteDatabase database = getWritableDatabase();

        for (String table : allTables){
            Cursor cursor = database.rawQuery("DROP TABLE IF EXISTS " + table, null);
            cursor.moveToFirst();
            cursor.close();
        }

        createTables(database);

        addRowSprechvokal_Substantiv("", "", "", "", "", "", "", "", "", "");

        addRowSprechvokal_Praesens("", "", "", "", "", "", "", "");

        addEntriesFromFile("db_initialisation/deklinationsendung.csv", DeklinationsendungDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/lektion.csv", LektionDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/personalendung_präsens.csv", Personalendung_PräsensDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/sprechvokal_Präsens.csv", Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/substantiv.csv", SubstantivDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/verb.csv", VerbDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/adverbTable.csv", AdverbDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/sprichwort.csv", SprichwortDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/präposition.csv", PräpositionDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/adjektiv.csv", AdjektivDB.FeedEntry.TABLE_NAME, context);
        addEntriesFromFile("db_initialisation/subjunktion.csv", SubjunktionDB.FeedEntry.TABLE_NAME, context);

        //TODO: Not final path/name
        addEntriesFromFile("example_sentences/beispielsatz_test.csv", BeispielsatzDB.FeedEntry.TABLE_NAME, context);

    }

    /**
     * Entries are added to a specified table from a file under a given path. (probably a .csv file)
     * 1 row represents a entry
     * the ';' separate the different columns of each entry
     * @param path    Path of the file relative to the 'assets' folder of the project
     * @param table   Name of the table where the entries are to be added to
     * @param context Application context
     */
    private void addEntriesFromFile(String path, String table, Context context){
        try{
            //Reading the file and getting a corresponding BufferedReader.
            InputStream inputStream = context.getAssets().open(path);
            InputStream bufferedInputStream = new BufferedInputStream(inputStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
            //Marking the beginning to jump back to it later.
            bufferedInputStream.mark(1000000000);

            //Skip the first line with column headings.
            bufferedReader.readLine();

            //Count the total number of lines in the file.
            int lineAmount = 0;
            while(bufferedReader.readLine() != null){
                lineAmount++;
            }

            //Reset to the beginning.
            bufferedInputStream.reset();
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

            //Skip the first line with column headings.
            bufferedReader.readLine();

            //Goes through every line and adds its content to the table.
            String line;
            for(int i = 0; i < lineAmount; i++){
                line = bufferedReader.readLine();
                line = General.replaceWithUmlaut(line);

                if(line != null){
                    String[] tokens = line.split(";", -1);

                    try {
                        //Checks for the wanted table and adds the row as a entry
                        switch (table) {

                            //Adverb
                            case AdverbDB.FeedEntry.TABLE_NAME:

                                addRowAdverb(tokens[0], tokens[1],
                                        Integer.parseInt(tokens[2]));

                                break;

                            case AdjektivDB.FeedEntry.TABLE_NAME:

                                addRowAdjektiv(tokens[0], tokens[1],
                                        Integer.parseInt(tokens[2]),
                                        tokens[3]);

                                break;

                            //Deklinationsendung
                            case DeklinationsendungDB.FeedEntry.TABLE_NAME:
                                addRowDeklinationsendung(tokens[0], tokens[1],
                                        tokens[2], tokens[3],
                                        tokens[4], tokens[5],
                                        tokens[6], tokens[7],
                                        tokens[8], tokens[9],
                                        tokens[10]);
                                break;

                            //Lektion
                            case LektionDB.FeedEntry.TABLE_NAME:
                                addRowLektion(Integer.parseInt(tokens[0]), tokens[1], tokens[2]);
                                break;

                            //Personalendung_Präsens
                            case Personalendung_PräsensDB.FeedEntry.TABLE_NAME:
                                addRowPersonalendung_Praesens(tokens[0], tokens[1],
                                        tokens[2], tokens[3],
                                        tokens[4], tokens[5]);
                                break;

                            //Präposition
                            case PräpositionDB.FeedEntry.TABLE_NAME:

                                addRowPraeposition(tokens[0], tokens[1],
                                        Integer.parseInt(tokens[2]));

                                break;

                            //Sprechvokal_Präsens
                            case Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME:
                                addRowSprechvokal_Praesens(tokens[0], tokens[1],
                                        tokens[2], tokens[3],
                                        tokens[4], tokens[5],
                                        tokens[6], tokens[7]);

                                break;

                            //Sprechvokal_Substantiv
                            case Sprechvokal_SubstantivDB.FeedEntry.TABLE_NAME:
                                //TODO: We don't know how the .csv file looks like -> add a entry here later
                                break;

                            //Sprichwort
                            case SprichwortDB.FeedEntry.TABLE_NAME:

                                addRowSprichwort(tokens[0], tokens[1],
                                        Integer.parseInt(tokens[2]));

                                break;

                            //Substantiv
                            case SubstantivDB.FeedEntry.TABLE_NAME:

                                //getting the deklination_ID
                                int deklinationId;
                                String query = "SELECT " + DeklinationsendungDB.FeedEntry._ID +
                                        " FROM " + DeklinationsendungDB.FeedEntry.TABLE_NAME +
                                        " WHERE " + DeklinationsendungDB.FeedEntry.COLUMN_NAME + " = ?";

                                SQLiteDatabase database = getWritableDatabase();
                                Cursor cursor = database.rawQuery(query,
                                        new String[]{tokens[3]}
                                );
                                cursor.moveToNext();
                                deklinationId = cursor.getInt(0);
                                cursor.close();

                                //TODO: Sprechvokale einfügen (nicht '1') -> momentan nur als placeholder here
                                addRowSubstantiv(tokens[0], tokens[1],
                                        Integer.parseInt(tokens[2]),
                                        1, deklinationId);
                                break;

                            //Verb
                            case VerbDB.FeedEntry.TABLE_NAME:

                                int personalendungID;
                                if (tokens[4].equals("no")){
                                    //Special case for stuff like 'esse'/...
                                    personalendungID = 2;
                                }else {
                                    personalendungID = 1;
                                }

                                int sprechvokalID;
                                String sprechvokalQuery = "SELECT " + Sprechvokal_PräsensDB.FeedEntry._ID +
                                        " FROM " + Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME +
                                        " WHERE " + Sprechvokal_PräsensDB.FeedEntry.COLUMN_TITLE + " = ?";
                                SQLiteDatabase db = getWritableDatabase();
                                Cursor sprechvokalCursor = db.rawQuery(sprechvokalQuery,
                                        new String[]{tokens[5]}
                                );
                                sprechvokalCursor.moveToNext();
                                sprechvokalID = sprechvokalCursor.getInt(0);
                                sprechvokalCursor.close();

                                addRowVerb(tokens[0], tokens[1],
                                        tokens[2],
                                        Integer.parseInt(tokens[3]),
                                        personalendungID, sprechvokalID);

                                break;


                            case BeispielsatzDB.FeedEntry.TABLE_NAME:

                                int subjekt_id = getIdOfVocabulary(tokens[0], DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG, SubstantivDB.FeedEntry.TABLE_NAME);
                                int praedikat_id = getIdOfVocabulary(tokens[1], "inf", VerbDB.FeedEntry.TABLE_NAME);
                                int gen_id = getIdOfVocabulary(tokens[2], DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG, SubstantivDB.FeedEntry.TABLE_NAME);
                                int dat_id = getIdOfVocabulary(tokens[3], DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG, SubstantivDB.FeedEntry.TABLE_NAME);
                                int akk_id = getIdOfVocabulary(tokens[4], DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG, SubstantivDB.FeedEntry.TABLE_NAME);

                                addRowBeispielsatz(subjekt_id, praedikat_id, gen_id, dat_id, akk_id);

                                break;

                            case SubjunktionDB.FeedEntry.TABLE_NAME:

                                addRowSubjunktion(tokens[0], tokens[1], Integer.parseInt(tokens[2]));
                                break;

                            default:
                                Log.e(TAG,
                                        "A table was not found while trying to add a entry from a file:\n" +
                                                "Method: DBHelper.class -> addEntriesFromFile(String path, String table, String context\n" +
                                                "Table: " + table);
                                break;
                        }
                    }catch (NumberFormatException nfe){
                        nfe.printStackTrace();

                    }catch (CursorIndexOutOfBoundsException e){
                        //This error might occur if the string in the initialisation file
                        //has a token that is not spelled correctly
                        //Example:
                        //o-deklination instead of o-deklination_n
                        String errorMessage =
                                "The input string from the initialisation file " +
                                        "contains a unexpected token (probably).\n" +
                                        "Printing content of 'tokens' below:\n";
                        for (String token : tokens){
                            errorMessage += token + ' ';
                        }
                        Log.e("UnexpectedToken", errorMessage);

                        e.printStackTrace();
                    }catch (IndexOutOfBoundsException e){
                        //This error might occur if the string in the initialisation file
                        //has less tokens than expected

                        String errorMessage =
                                "The input string from the initialisation file " +
                                        "has too few tokens (probably).\n" +
                                        "Printing content of 'tokens' below:\n";
                        for (String token : tokens){
                            errorMessage += token + ' ';
                        }

                        Log.e("StringHasTooFewTokens", errorMessage);
                        e.printStackTrace();
                    }


                }
            }
            inputStream.close();
            bufferedInputStream.close();
            bufferedReader.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    //
    //Methods for modifying database content
    //

    private static void createTables(SQLiteDatabase db){
        //Creating the tables in the database
        db.execSQL(SQL_CREATE_ENTRIES_ADVERB);
        db.execSQL(SQL_CREATE_ENTRIES_ADJEKTIV);
        db.execSQL(SQL_CREATE_ENTRIES_DEKLINATIONSENDUNG);
        db.execSQL(SQL_CREATE_ENTRIES_LEKTION);
        db.execSQL(SQL_CREATE_ENTRIES_PERSONALENDUNG_PRÄSENS);
        db.execSQL(SQL_CREATE_ENTRIES_PRAEPOSITION);
        db.execSQL(SQL_CREATE_ENTRIES_SPRECHVOKAL_PRÄSENS);
        db.execSQL(SQL_CREATE_ENTRIES_SPRECHVOKAL_SUBSTANTIV);
        db.execSQL(SQL_CREATE_ENTRIES_SPRICHWORT);
        db.execSQL(SQL_CREATE_ENTRIES_SUBSTANTIV);
        db.execSQL(SQL_CREATE_ENTRIES_VERB);
        db.execSQL(SQL_CREATE_ENTRIES_BEISPIELSATZ);
        db.execSQL(SQL_CREATE_ENTRIES_SUBJUNKTION);
    }

    private static void deleteEntries(SQLiteDatabase db){

        //Deletes all entries
        db.execSQL(SQL_DELETE_ENTRIES_ADVERB);
        db.execSQL(SQL_DELETE_ENTRIES_ADJEKTIV);
        db.execSQL(SQL_DELETE_ENTRIES_DEKLINATIONSENDUNG);
        db.execSQL(SQL_DELETE_ENTRIES_LEKTION);
        db.execSQL(SQL_DELETE_ENTRIES_PERSONALENDUNG_PRÄSENS);
        db.execSQL(SQL_DELETE_ENTRIES_PRAEPOSITION);
        db.execSQL(SQL_DELETE_ENTRIES_SPRECHVOKAL_PRÄSENS);
        db.execSQL(SQL_DELETE_ENTRIES_SPRECHVOKAL_SUBSTANTIV);
        db.execSQL(SQL_DELETE_ENTRIES_SPRICHWORT);
        db.execSQL(SQL_DELETE_ENTRIES_SUBSTANTIV);
        db.execSQL(SQL_DELETE_ENTRIES_VERB);
        db.execSQL(SQL_DELETE_ENTRIES_BEISPIELSATZ);
        db.execSQL(SQL_DELETE_ENTRIES_SUBJUNKTION);
    }

    /**
     * Sets the 'gelernt' value of a single entry
     * @param tableName Name of the table where the vocabulary is in
     * @param vokabelID _ID of the vocabulary
     * @param gelerntWert value of 'gelernt' that the vocabulary is to be set to
     */
    public void setGelernt(String tableName, int vokabelID, boolean gelerntWert){

        String query = "UPDATE " + tableName +
                " SET Gelernt = ?" +
                " WHERE _ID = ?";

        Cursor cursor = getWritableDatabase().rawQuery(query, new String [] {(gelerntWert ? "1" : "0"),Integer.toString(vokabelID)});
        //We need to call 'moveToFirst()' or 'moveToNext()' here for the database to properly update
        cursor.moveToFirst();

        cursor.close();
    }

    public void incrementValue(String table, String column, int entryID){

        String query =
                "UPDATE " + table +
                " SET " + column + " = " + column + " + 1" +
                " WHERE _id = " + entryID;

        Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{});
        cursor.moveToFirst();
        cursor.close();
    }

    /**
     * Resets the 'gelernt' status of all vocabularies in a 'lektion' back to false
     * @param lektionNr the 'lektion' that is to be reset
     */
    public void resetLektion(int lektionNr){

        for(String table: allVocabularyTables){
            String query = "UPDATE "
                    + table
                    + " SET Gelernt = 0, Amount_Incorrect = 0"
                    + " WHERE Lektion_ID = " + lektionNr;
            Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{});
            cursor.moveToFirst();
            cursor.close();
        }

    }

    public int getMistakeAmount(int lektion){

        int count = 0;

        for(String table: allVocabularyTables){
            String query = "SELECT SUM(Amount_Incorrect) FROM " + table + " WHERE Lektion_ID = " + lektion;
            Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{});
            cursor.moveToFirst();

            count += cursor.getInt(0);

            cursor.close();
        }

        return count;
    }

    //
    // Methods for getting data from the database
    //

    /**
     * Gets a specific column from a table-entry
     * @param id _ID of the entry
     * @param table table of the entry
     * @param column wanted column
     * @return a String of the content of the entry in the wanted column
     */
    public String getColumnFromId(int id, String table, String column){

        String query = "SELECT "+ column +
                " FROM " + table +
                " WHERE _ID = ?";

        Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{Integer.toString(id)});

        cursor.moveToNext();

        String columnContent = cursor.getString(0);

        cursor.close();

        return columnContent;
    }

    /**
     * Gets a percentage of how many vocabularies in a specific 'Lektion' have been completed
     * @param lektion the lektion where a percentage is wanted
     * @return a percentage of how many vocabularies have been learned. Range: 0 to 1
     */
    public float getGelerntProzent(int lektion){

        int entryAmout = countTableEntries(allVocabularyTables, lektion);

        //Returning -1 if the database doesn't have any entries to avoid division by 0
        if (entryAmout == 0){
            Log.e("getGelerntProzent", "Division by 0: no entries in lektion " + lektion);
            return -1;
        }

        int entryAmountGelernt = countTableEntries(allVocabularyTables, lektion, true);


        return ((float)entryAmountGelernt/entryAmout);
    }

    /**
     * Returning all wanted columns of a table with a passed value for 'Lektion'
     * @param table the table from which the entries are wanted
     * @param columns array of wanted columns
     * @param lektion the wanted lektion
     * @return a 2D-array containing the all entries (1st D) & the wanted columns (2nd D)
     */
    public String[][] getColumns(String table, String[] columns, int lektion){

        //Creating the 'query'-String
        StringBuilder stringBuilder = new StringBuilder(63);
        stringBuilder.append("SELECT ");
        for (int i = 0; i < columns.length; i++){
            stringBuilder.append(columns[i]);
            if (i < columns.length-1){
                stringBuilder.append(',');
            }
            stringBuilder.append(' ');
        }
        String query = stringBuilder.toString();
        query += "FROM " + table + " WHERE Lektion_ID = " + lektion;

        //Getting the result-cursor
        Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{});

        //Putting the results in a 2-dimensional array
        //where the first dimension is the entryNr
        //and the second dimension a list of the wanted columns
        String[][] values = new String[cursor.getCount()][columns.length];
        int count = 0;
        while (cursor.moveToNext()){
            for (int i = 0; i < columns.length; i++){
                values[count][i] = cursor.getString(i);
            }
            count++;
        }

        cursor.close();

        return values;
    }

    /**
     * Gets a vocabulary at the 'count'-position in a table with a specific 'gelernt'-value
     * @param lektion the 'lektion' where the vocabularies are from
     * @param count the amount of vocabularies previous to this one - 1
     * @param gelernt 'gelernt'-value
     * @param table table of the wanted vocabulary
     * @return the _ID of the selected vocabulary
     */
    private int getIdFromCount(int lektion, int count, boolean gelernt, String table){

        String query = "SELECT _ID FROM "+ table
                + " WHERE Gelernt = "+(gelernt ? 1 : 0)
                + " AND Lektion_ID = " + lektion;
        Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{});

        cursor.moveToPosition(count-1);

        int id = cursor.getInt(0);

        cursor.close();

        return id;
    }

    /**
     * Gets a vocabulary at the 'count'-position in a table with a specific 'gelernt'-value
     * @param count the amount of vocabularies previous to this one - 1
     * @param table table of the wanted vocabulary
     * @return the _ID of the selected vocabulary
     */
    private int getIdFromCount(int count, String table){

        String query = "SELECT _ID FROM "+ table;
        Cursor cursor = getWritableDatabase().rawQuery(query, new String[]{});

        cursor.moveToPosition(count-1);

        int id = cursor.getInt(0);

        cursor.close();

        return id;
    }

    /**
     * Searches through a table to find the _ID value of a vocabulary
     *
     * TODO: Currently only VerbDB and SubstantivDB can be targeted
     *
     * @param vocabulary The vocabulary where the corresponding _ID is needed
     * @param currentCase current declination/konjugation of the vocabulary (Nom/Akk/Inf)
     * @param targetTable The table where the vocabulary is supposed to be in
     * @return the _ID value of the vocabulary (-1 for no result)
     */
    private int getIdOfVocabulary(String vocabulary, String currentCase, String targetTable){

        int entryAmount = countTableEntries(targetTable);

        if (targetTable.equals(SubstantivDB.FeedEntry.TABLE_NAME)){

            for (int id = 1; id <= entryAmount; id++){
                if (vocabulary.equals(getDekliniertenSubstantiv(id, currentCase))) return id;
            }

        }else if (targetTable.equals(VerbDB.FeedEntry.TABLE_NAME)){

            for (int id = 1; id <= entryAmount; id++){
                if (vocabulary.equals(getKonjugiertesVerb(id, currentCase))) return id;
            }

        }else {
            Log.e("TableNotFound", "The targetted table in \"getIdOfVocabulary(..)\" was not found.\n"
                    +"targetTable: " + targetTable +"\n"
                    +"vocabulary: " + vocabulary + "\n"
                    +"currentCase: " + currentCase);
        }

        return -1;
    }


    //
    // Methods for getting forms of vocabularies
    //

    /**
     * Returns a noun in the wanted declination
     * @param vokabelID _ID of the entry in the table 'Substantiv'
     * @param deklinationsendungsName deklination of the wanted substantiv (from Deklinationsendung)
     * @return the final word in the right declination
     */
    public String getDekliniertenSubstantiv(int vokabelID, String deklinationsendungsName){

        SQLiteDatabase database = getWritableDatabase();

        //Gets the first part of the word (wortstamm)
        String query = "SELECT " + SubstantivDB.FeedEntry.COLUMN_WORTSTAMM +
                " FROM "+ SubstantivDB.FeedEntry.TABLE_NAME +
                " WHERE _ID = ?";
        Cursor substantivCursor = database.rawQuery(query, new String[] {Integer.toString(vokabelID)});
        substantivCursor.moveToNext();
        String wortstamm = substantivCursor.getString(0);
        substantivCursor.close();

        //TODO: 'Sprechvokal' is missing because there haven't been any vocabulary having one yet.

        //gets the last part of the word (endung)
        query = "SELECT "
                + DeklinationsendungDB.FeedEntry.TABLE_NAME+'.'+deklinationsendungsName +
                " FROM " +
                DeklinationsendungDB.FeedEntry.TABLE_NAME + ", " +
                SubstantivDB.FeedEntry.TABLE_NAME +
                " WHERE " +
                SubstantivDB.FeedEntry.TABLE_NAME+'.'+SubstantivDB.FeedEntry._ID +
                " = " +
                '?' +
                " AND " +
                SubstantivDB.FeedEntry.TABLE_NAME+'.'+SubstantivDB.FeedEntry.COLUMN_DEKLINATIONSENDUNG_ID +
                " = " +
                DeklinationsendungDB.FeedEntry.TABLE_NAME+'.'+DeklinationsendungDB.FeedEntry._ID;
        Cursor endungCursor = database.rawQuery(query, new String[] {Integer.toString(vokabelID)}
        );
        endungCursor.moveToNext();
        String endung = endungCursor.getString(0);
        endungCursor.close();

        return (wortstamm + endung);

    }

    /**
     *
     * @param vokabelID _ID of the entry in the table 'Adjektiv'
     * @param deklinationsendungName deklination of the wanted adjectiv (a column from Deklinationsendung)
     * @param endungstype Many adjektives can be masculine/feminin/... -> indicate which one
     *                    with the identifyer 'a-Deklination / o-Deklination_m / ...' and so on
     * @return the final word in the right declination
     */
    public String getDekliniertesAdjektiv(int vokabelID, String deklinationsendungName, String endungstype){

        SQLiteDatabase database = getWritableDatabase();

        //Gets the first part of the word (wortstamm)
        String query = "SELECT " + AdjektivDB.FeedEntry.COLUMN_WORTSTAMM +
                " FROM "+ AdjektivDB.FeedEntry.TABLE_NAME +
                " WHERE _ID = ?";
        Cursor adjektivCursor = database.rawQuery(query, new String[] {Integer.toString(vokabelID)});
        adjektivCursor.moveToNext();
        String wortstamm = adjektivCursor.getString(0);
        adjektivCursor.close();


        //gets the last part of the word (endung)

        //FIXME: Adjectives not final! (multiline TODO)
        //We need to check for special cases where the adjective
        //cannot be m/f/n anymore -> special cases.
        //We might also need to expand our database for this
        query = "SELECT "
                + DeklinationsendungDB.FeedEntry.TABLE_NAME+'.'+deklinationsendungName +
                " FROM " +
                DeklinationsendungDB.FeedEntry.TABLE_NAME + ", " +
                AdjektivDB.FeedEntry.TABLE_NAME +
                " WHERE " +
                AdjektivDB.FeedEntry.TABLE_NAME+'.'+AdjektivDB.FeedEntry._ID +
                " = " +
                '?' +
                " AND " +
                '\''+endungstype+'\'' +
                " = " +
                DeklinationsendungDB.FeedEntry.TABLE_NAME+'.'+DeklinationsendungDB.FeedEntry.COLUMN_NAME;
        Cursor endungCursor = database.rawQuery(query, new String[] {Integer.toString(vokabelID)}
        );
        endungCursor.moveToNext();
        String endung = endungCursor.getString(0);
        endungCursor.close();

        return (wortstamm+endung);
    }

    /**
     * Returns a verb in the wanted konjugation
     * @param vokabelID _ID of the entry in the table 'Verb'
     * @param personalendung konjugation of the wanted verb (from Personalendung_Präsens/"Inf",... for infinitiv)
     * @return the final word in the right declination
     */
    public String getKonjugiertesVerb(int vokabelID, String personalendung){

        SQLiteDatabase database = getWritableDatabase();

        //TODO: add parameter for tenses

        //Gets the first part of the word (Wortstamm)
        String query = "SELECT * FROM " + VerbDB.FeedEntry.TABLE_NAME + " WHERE _ID = ?";
        Cursor verbCursor = database.rawQuery(query, new String[] {Integer.toString(vokabelID)});
        verbCursor.moveToNext();
        String verbStamm = verbCursor.getString(verbCursor.getColumnIndex("Wortstamm"));
        verbCursor.close();


        String sprechvokal;
        String endung;

        if (personalendung.equals("inf") || personalendung.equals("infinitiv") ||
                personalendung.equals("Inf") || personalendung.equals("Infinitiv")){


            String sprechvokalQuery = "SELECT "
                    + Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME+'.'+Sprechvokal_PräsensDB.FeedEntry.COLUMN_INFINITV +
                    " FROM " +
                    Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME + ", " +
                    VerbDB.FeedEntry.TABLE_NAME +
                    " WHERE " +
                    VerbDB.FeedEntry.TABLE_NAME+'.'+VerbDB.FeedEntry._ID +
                    " = " +
                    vokabelID +
                    " AND " +
                    VerbDB.FeedEntry.TABLE_NAME+'.'+VerbDB.FeedEntry.COLUMN_SPRECHVOKAL_ID +
                    " = " +
                    Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME+'.'+Sprechvokal_PräsensDB.FeedEntry._ID;
            Cursor sprechvokalCursor = database.rawQuery(sprechvokalQuery, new String[]{});
            sprechvokalCursor.moveToNext();
            sprechvokal = sprechvokalCursor.getString(0);
            sprechvokalCursor.close();

            String konjugationQuery = "SELECT " + VerbDB.FeedEntry.COLUMN_KONJUGATION +
                    " FROM " + VerbDB.FeedEntry.TABLE_NAME +
                    " WHERE _ID = " + vokabelID;
            Cursor konjugationCursor = database.rawQuery(konjugationQuery, new String[]{});
            konjugationCursor.moveToNext();
            String konjugation = konjugationCursor.getString(0);
            konjugationCursor.close();

            if (konjugation.equals("special")) {
                endung = "";
            }else{
                endung = "re";
            }

        }else{
            //gets the middle part of the word (Sprechvokal)
            query = "SELECT "
                    + Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME+'.'+personalendung +
                    " FROM " +
                    Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME + ", " +
                    VerbDB.FeedEntry.TABLE_NAME +
                    " WHERE " +
                    VerbDB.FeedEntry.TABLE_NAME+'.'+VerbDB.FeedEntry._ID +
                    " = " +
                    vokabelID +
                    " AND " +
                    VerbDB.FeedEntry.TABLE_NAME+'.'+VerbDB.FeedEntry.COLUMN_SPRECHVOKAL_ID +
                    " = " +
                    Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME+'.'+Sprechvokal_PräsensDB.FeedEntry._ID;
            Cursor sprechvokalCursor = database.rawQuery(query, new String[]{});
            sprechvokalCursor.moveToNext();
            sprechvokal = sprechvokalCursor.getString(sprechvokalCursor.getColumnIndex(personalendung));
            sprechvokalCursor.close();

            //Gets the last part of the word (Endung)
            query = "SELECT "
                    + Personalendung_PräsensDB.FeedEntry.TABLE_NAME+'.'+personalendung+
                    " FROM " +
                    Personalendung_PräsensDB.FeedEntry.TABLE_NAME + ", " +
                    VerbDB.FeedEntry.TABLE_NAME +
                    " WHERE " +
                    VerbDB.FeedEntry.TABLE_NAME+'.'+VerbDB.FeedEntry._ID +
                    " = " +
                    "?" +
                    " AND " +
                    VerbDB.FeedEntry.TABLE_NAME+'.'+VerbDB.FeedEntry.COLUMN_PERSONALENDUNG_ID +
                    " = " +
                    Personalendung_PräsensDB.FeedEntry.TABLE_NAME+'.'+Personalendung_PräsensDB.FeedEntry._ID;
            Cursor personalendungCursor = database.rawQuery(query , new String[] { Integer.toString(vokabelID)}
            );
            personalendungCursor.moveToNext();
            endung = personalendungCursor.getString(personalendungCursor.getColumnIndex(personalendung));
            personalendungCursor.close();
        }

        return (verbStamm + sprechvokal + endung);
    }


    //
    // Methods for counting table entries
    //

    public int countTableEntries(String[] tables){

        String query = "SELECT COUNT(*) FROM (";

        for(int i = 0; i < tables.length; i++){
            query += "SELECT _ID FROM " + tables[i];
            if(i != tables.length-1){
                query += " UNION ALL ";
            }
        }

        query += ")";

        Cursor cursor = getWritableDatabase().rawQuery(query, null);

        cursor.moveToNext();

        int count = cursor.getInt(0);

        cursor.close();

        return count;
    }

    /**
     * Counts the entries of all tables in the tables Array with a specific 'Lektion_id'.
     * Only works if every table of the array has a foreign key 'Lektion_id'.
     * @param tables Array of all tables, where the entries are to be counted
     * @param lektionNr the id of the 'lektion', where the entries are to be counted
     * @return the amount of entries in the tables of the array; returns -1 if the any table doesn't have 'Lektion_id' as foreign key
     */
    public int countTableEntries(String[] tables, int lektionNr){

        //TODO: NOT YET TESTED, JUST CPY&PASTED FROM ABOVE ->INPORTANT

        String query = "SELECT COUNT(*) FROM (";

        for(int i = 0; i < tables.length; i++){
            query += "SELECT _ID FROM " + tables[i] + " WHERE Lektion_ID = " + lektionNr;
            if(i != tables.length-1){
                query += " UNION ALL ";
            }
        }

        query += ")";

        Cursor cursor = getWritableDatabase().rawQuery(query, null);

        cursor.moveToNext();

        int count = cursor.getInt(0);

        cursor.close();

        return count;
    }

    /**
     * Counts the entries of all tables in the tables Array with a specific 'Lektion_id' and a 'gelernt' value
     * Only works if every table of the array has a foreign key 'Lektion_id'/'Gelernt'.
     * @param tables Array of all tables, where the entries are to be counted
     * @param lektionNr the id of the 'lektion', where the entries are to be counted
     * @param gelernt should the requested vokabel be 'gelernt==true'
     * @return the amount of entries in the tables of the array; returns -1 if the any table doesn't have 'Lektion_id' as foreign key
     */
    private int countTableEntries(String[] tables, int lektionNr, boolean gelernt){
        String query = "SELECT COUNT(*) FROM (";

        for(int i = 0; i < tables.length; i++){
            query += "SELECT _ID FROM " + tables[i] + " WHERE Lektion_ID = " + lektionNr + " AND Gelernt = " + (gelernt ? 1 : 0);
            if(i != tables.length-1){
                query += " UNION ALL ";
            }
        }

        query += ")";

        Cursor cursor = getWritableDatabase().rawQuery(query, null);

        cursor.moveToNext();

        int count = cursor.getInt(0);

        cursor.close();

        return count;
    }


    /**
     * Counts the entries of all tables in the tables Array with a specific 'Lektion_id' and a 'gelernt' value
     * Only works if every table of the array has a foreign key 'Lektion_id'/'Gelernt'.
     * @param table table that is to be counted
     * @param lektionNr the id of the 'lektion', where the entries are to be counted
     * @param gelernt should the requested vokabel be 'gelernt==true'
     * @return the amount of entries in the tables of the array; returns -1 if the any table doesn't have 'Lektion_id' as foreign key
     */
    private int countTableEntries(String table, int lektionNr, boolean gelernt){

        //getting the total number of entries which were completed and adding it to 'complete'
        String query = "SELECT COUNT(*) FROM " + table
                + " WHERE Lektion_ID = ?" +
                " AND Gelernt = ?";

        Cursor cursor = getWritableDatabase().rawQuery(query,
                new String[] {Integer.toString(lektionNr), (gelernt ? "1" : "0")});
        cursor.moveToNext();
        int count = cursor.getInt(0);

        cursor.close();

        return count;
    }

    /**
     * Returns the amount of entries in all tables passed in the String-array
     * @param table table that is to be counted
     * @return the amount of entries in the tables of the array
     */
    public int countTableEntries(String table){

        //Getting the total number of entries which were completed and adding it to 'complete'
        String query = "SELECT COUNT(*) FROM " + table;
        Cursor cursor = getWritableDatabase().rawQuery(query,
                new String[] {});
        cursor.moveToNext();
        int result = cursor.getInt(0);
        cursor.close();

        return result;
    }


    //
    // Methods for getting randomized vocabularies
    //

    /**
     * Choses a random vocabulary from a lektion
     * @param lektionNr 'lektion' where the entry is chose from
     * @return a instance of the chosen vocabulary as object
     */
    public Vokabel getRandomVocabulary(int lektionNr){

        //Get the amount of entries with a matching lektionNr
        int entryAmountVerb = countTableEntries(VerbDB.FeedEntry.TABLE_NAME, lektionNr,false);
        int entryAmountAdjektiv = countTableEntries(AdjektivDB.FeedEntry.TABLE_NAME, lektionNr, false);
        int entryAmountSubstantiv = countTableEntries(SubstantivDB.FeedEntry.TABLE_NAME, lektionNr, false);
        int entryAmountPraeposition = countTableEntries(PräpositionDB.FeedEntry.TABLE_NAME, lektionNr, false);
        int entryAmountSprichwort = countTableEntries(SprichwortDB.FeedEntry.TABLE_NAME, lektionNr, false);
        int entryAmountAdverb = countTableEntries(AdverbDB.FeedEntry.TABLE_NAME, lektionNr, false);
        int entryAmountSubjunktion = countTableEntries(SubjunktionDB.FeedEntry.TABLE_NAME, lektionNr, false);
        int entryAmountTotal = entryAmountSubstantiv + entryAmountAdjektiv + entryAmountVerb + entryAmountPraeposition + entryAmountSprichwort + entryAmountAdverb + entryAmountSubjunktion;

        Random rand = new Random();
        int randomNumber = rand.nextInt(entryAmountTotal);
        String lateinVokabel;
        String table;
        int count;
        int vokabelID;
        String deutsch;
        Vokabel vokabelInstance;

        if(randomNumber < entryAmountSubstantiv){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Substantiv from the given randomNumber
            count = randomNumber;

            table = SubstantivDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getDekliniertenSubstantiv(vokabelID, DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG);
            deutsch = getColumnFromId(vokabelID, table, SubstantivDB.FeedEntry.COLUMN_NOM_SG_DEUTSCH);

            vokabelInstance = new SubstantivDB(vokabelID, lateinVokabel, deutsch);

        } else if (randomNumber-entryAmountSubstantiv < entryAmountVerb){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Verb from the given randomNumber
            count = randomNumber-entryAmountSubstantiv;

            table = VerbDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getKonjugiertesVerb(vokabelID,"inf");
            deutsch = getColumnFromId(vokabelID, table, VerbDB.FeedEntry.COLUMN_INFINITIV_DEUTSCH);

            vokabelInstance = new VerbDB(vokabelID, lateinVokabel, deutsch);

        }else if (randomNumber-entryAmountSubstantiv-entryAmountVerb < entryAmountPraeposition){
            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Präposition from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb;
            table = PräpositionDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getColumnFromId(vokabelID, table, PräpositionDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, PräpositionDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new PräpositionDB(vokabelID, lateinVokabel, deutsch);

        }else if (randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition < entryAmountSprichwort){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Sprichwort from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition;

            table = SprichwortDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getColumnFromId(vokabelID, table, SprichwortDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, SprichwortDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new SprichwortDB(vokabelID, lateinVokabel, deutsch);

        }else if(randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort < entryAmountAdverb){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;


            //constructs a instance of Adverb from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort;

            table = AdverbDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getColumnFromId(vokabelID, table, AdverbDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, AdverbDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new AdverbDB(vokabelID, lateinVokabel, deutsch);

        }else if(randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdverb < entryAmountAdjektiv){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Adjektiv from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdverb;

            table = AdjektivDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getDekliniertesAdjektiv(vokabelID,
                    DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG,
                    "o-Deklination_m") + ", a, um";
            deutsch = getColumnFromId(vokabelID, table, AdjektivDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new AdjektivDB(vokabelID, lateinVokabel, deutsch);

        }else if (randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdjektiv - entryAmountAdverb < entryAmountSubjunktion){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Adjektiv from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdverb-entryAmountAdjektiv;

            table = SubjunktionDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(lektionNr, count, false, table);
            lateinVokabel = getColumnFromId(vokabelID, table, SubjunktionDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, SubjunktionDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new SubjunktionDB(vokabelID, lateinVokabel, deutsch);

        }else {
            Log.e(DBHelper.class.getName(), "entry_id given by the randomNumber is out of bounds -> bigger than the amount of all entries combined");
            return null;
        }

        return vokabelInstance;
    }

    /**
     * Choses a random vocabulary from a lektion
     * @return a instance of the chosen vocabulary as object
     */
    public Vokabel getRandomVocabulary(){

        //Get the amount of entries with a matching lektionNr
        int entryAmountVerb = countTableEntries(VerbDB.FeedEntry.TABLE_NAME);
        int entryAmountAdjektiv = countTableEntries(AdjektivDB.FeedEntry.TABLE_NAME);
        int entryAmountSubstantiv = countTableEntries(SubstantivDB.FeedEntry.TABLE_NAME);
        int entryAmountPraeposition = countTableEntries(PräpositionDB.FeedEntry.TABLE_NAME);
        int entryAmountSprichwort = countTableEntries(SprichwortDB.FeedEntry.TABLE_NAME);
        int entryAmountAdverb = countTableEntries(AdverbDB.FeedEntry.TABLE_NAME);
        int entryAmountSubjunktion = countTableEntries(SubjunktionDB.FeedEntry.TABLE_NAME);
        int entryAmountTotal = entryAmountSubstantiv + entryAmountAdjektiv + entryAmountVerb + entryAmountPraeposition + entryAmountSprichwort + entryAmountAdverb + entryAmountSubjunktion;

        Random rand = new Random();
        int randomNumber = rand.nextInt(entryAmountTotal);
        String lateinVokabel;
        String table;
        int count;
        int vokabelID;
        String deutsch;
        Vokabel vokabelInstance;

        if(randomNumber < entryAmountSubstantiv){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Substantiv from the given randomNumber
            count = randomNumber;

            table = SubstantivDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getDekliniertenSubstantiv(vokabelID, DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG);
            deutsch = getColumnFromId(vokabelID, table, SubstantivDB.FeedEntry.COLUMN_NOM_SG_DEUTSCH);

            vokabelInstance = new SubstantivDB(vokabelID, lateinVokabel, deutsch);

        } else if (randomNumber-entryAmountSubstantiv < entryAmountVerb){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Verb from the given randomNumber
            count = randomNumber-entryAmountSubstantiv;

            table = VerbDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getKonjugiertesVerb(vokabelID,"inf");
            deutsch = getColumnFromId(vokabelID, table, VerbDB.FeedEntry.COLUMN_INFINITIV_DEUTSCH);

            vokabelInstance = new VerbDB(vokabelID, lateinVokabel, deutsch);

        }else if (randomNumber-entryAmountSubstantiv-entryAmountVerb < entryAmountPraeposition){
            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Präposition from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb;
            table = PräpositionDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getColumnFromId(vokabelID, table, PräpositionDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, PräpositionDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new PräpositionDB(vokabelID, lateinVokabel, deutsch);

        }else if (randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition < entryAmountSprichwort){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Sprichwort from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition;

            table = SprichwortDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getColumnFromId(vokabelID, table, SprichwortDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, SprichwortDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new SprichwortDB(vokabelID, lateinVokabel, deutsch);

        }else if(randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort < entryAmountAdverb){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;


            //constructs a instance of Adverb from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort;

            table = AdverbDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getColumnFromId(vokabelID, table, AdverbDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, AdverbDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new AdverbDB(vokabelID, lateinVokabel, deutsch);

        }else if(randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdverb < entryAmountAdjektiv){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Adjektiv from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdverb;

            table = AdjektivDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getDekliniertesAdjektiv(vokabelID,
                    DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG,
                    "o-Deklination_m") + ", a, um";
            deutsch = getColumnFromId(vokabelID, table, AdjektivDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new AdjektivDB(vokabelID, lateinVokabel, deutsch);

        }else if (randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdjektiv - entryAmountAdverb < entryAmountSubjunktion){

            //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
            randomNumber++;

            //constructs a instance of Adjektiv from the given randomNumber
            count = randomNumber-entryAmountSubstantiv-entryAmountVerb-entryAmountPraeposition-entryAmountSprichwort-entryAmountAdverb-entryAmountAdjektiv;

            table = SubjunktionDB.FeedEntry.TABLE_NAME;
            vokabelID = getIdFromCount(count, table);
            lateinVokabel = getColumnFromId(vokabelID, table, SubjunktionDB.FeedEntry.COLUMN_LATEIN);
            deutsch = getColumnFromId(vokabelID, table, SubjunktionDB.FeedEntry.COLUMN_DEUTSCH);

            vokabelInstance = new SubjunktionDB(vokabelID, lateinVokabel, deutsch);

        }else {
            Log.e(DBHelper.class.getName(), "entry_id given by the randomNumber is out of bounds -> bigger than the amount of all entries combined");
            return null;
        }

        return vokabelInstance;
    }

    public SubstantivDB getRandomSubstantiv(){

        //TODO: Only pick from learned words if enough are learned
        int entryAmountSubstantiv = countTableEntries(SubstantivDB.FeedEntry.TABLE_NAME);

        SubstantivDB substantivInstance;
        String table = SubstantivDB.FeedEntry.TABLE_NAME;
        String lateinVokabel;
        String deutsch;


        int randomNumber = new Random().nextInt(entryAmountSubstantiv);
        //Incrementing count by 1 since id starts with 1 not 0
        randomNumber++;



        //FIXME: this might not be true if we have a more specialized search -> lektion/learned
        int vokabelID = randomNumber;
        lateinVokabel = getDekliniertenSubstantiv(vokabelID, DeklinationsendungDB.FeedEntry.COLUMN_NOM_SG);
        deutsch = getColumnFromId(vokabelID, table, SubstantivDB.FeedEntry.COLUMN_NOM_SG_DEUTSCH);

        substantivInstance = new SubstantivDB(vokabelID, lateinVokabel, deutsch);

        return substantivInstance;
    }

    /**
     * Returns a instance of a random 'Verb'-entry with a specific 'lektion'
     * @param lektionNr the 'lektion' where the 'Verb' should be from
     * @return a instance of the 'Verb'
     */
    public VerbDB getRandomVerb(int lektionNr){

        int entryAmountVerb = countTableEntries(VerbDB.FeedEntry.TABLE_NAME, lektionNr, false);

        Random rand = new Random();
        int randomNumber = rand.nextInt(entryAmountVerb);
        String lateinVokabel;
        String table;
        int count;
        int vokabelID;
        String deutsch;
        VerbDB verbInstance;

        //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
        randomNumber++;

        //constructs a instance of Verb from the given randomNumber
        count = randomNumber;

        table = VerbDB.FeedEntry.TABLE_NAME;
        vokabelID = getIdFromCount(lektionNr, count, false, table);
        lateinVokabel = getKonjugiertesVerb(vokabelID, "inf");
        deutsch = getColumnFromId(vokabelID, table, VerbDB.FeedEntry.COLUMN_INFINITIV_DEUTSCH);

        verbInstance = new VerbDB(vokabelID, lateinVokabel, deutsch);

        return verbInstance;
    }

    /**
     * Returns a instance of a random 'Verb'-entry with a specific 'lektion'
     * @return a instance of the 'Verb'
     */
    public VerbDB getRandomVerb(){

        int entryAmountVerb = countTableEntries(VerbDB.FeedEntry.TABLE_NAME);

        String lateinVokabel;
        String table = VerbDB.FeedEntry.TABLE_NAME;
        String deutsch;
        VerbDB verbInstance;

        int randomNumber = new Random().nextInt(entryAmountVerb);
        //increments randomNumber by 1 because _ID in the tables starts with '1' not '0'
        randomNumber++;



        //FIXME: this might not be true if we have a more specialized search -> lektion/learned
        int vokabelID = randomNumber;

        lateinVokabel = getKonjugiertesVerb(vokabelID, "inf");
        deutsch = getColumnFromId(vokabelID, table, VerbDB.FeedEntry.COLUMN_INFINITIV_DEUTSCH);

        verbInstance = new VerbDB(vokabelID, lateinVokabel, deutsch);

        return verbInstance;
    }


    //
    // Methods for adding entries to the database
    //

    /**
     * Adds a entry to the 'Adverb' table in the database with given parameters.
     * @param deutsch    content of the column 'deutsch' in the database entry
     * @param latein     content of the column 'latein' in the database entry
     * @param lektion_id foreign key: the corresponding entry from the 'Lektion' table
     */
    private void addRowAdverb(String deutsch, String latein, int lektion_id){

        ContentValues values = new ContentValues();
        values.put(allColumnsAdverb[1], deutsch);
        values.put(allColumnsAdverb[2], latein);
        values.put(allColumnsAdverb[3], 0);
        values.put(allColumnsAdverb[4], 0);
        values.put(allColumnsAdverb[5], lektion_id);

        getWritableDatabase().insert(AdverbDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Adverb' table in the database with given parameters.
     * @param deutsch    content of the column 'deutsch' in the database entry
     * @param latein     content of the column 'latein' in the database entry
     * @param lektion_id foreign key: the corresponding entry from the 'Lektion' table
     * @param type       content of the column 'type' in the database entry
     */
    private void addRowAdjektiv(String deutsch, String latein, int lektion_id, String type){

        ContentValues values = new ContentValues();
        values.put(allColumnsAdjektiv[1], deutsch);
        values.put(allColumnsAdjektiv[2], latein);
        values.put(allColumnsAdjektiv[3], 0);
        values.put(allColumnsAdjektiv[4], 0);
        values.put(allColumnsAdjektiv[5], lektion_id);
        values.put(allColumnsAdjektiv[6], type);

        getWritableDatabase().insert(AdjektivDB.FeedEntry.TABLE_NAME, null, values);

    }

    /**
     * Adds a entry to the 'Deklinationsendung' table in the database with given parameters.
     * @param name   content of the column 'name' in the database entry
     * @param nom_sg content of the column 'nom_sg' in the database entry
     * @param nom_pl content of the column 'nom_pl' in the database entry
     * @param gen_sg content of the column 'gen_sg' in the database entry
     * @param gen_pl content of the column 'gen_pl' in the database entry
     * @param dat_sg content of the column 'dat_sg' in the database entry
     * @param dat_pl content of the column 'dat_pl' in the database entry
     * @param akk_sg content of the column 'akk_sg' in the database entry
     * @param akk_pl content of the column 'akk_pl' in the database entry
     * @param abl_sg content of the column 'abl_sg' in the database entry
     * @param abl_pl content of the column 'abl_pl' in the database entry
     */
    private void addRowDeklinationsendung(String name,
                                          String nom_sg, String nom_pl,
                                          String gen_sg, String gen_pl,
                                          String dat_sg, String dat_pl,
                                          String akk_sg, String akk_pl,
                                          String abl_sg, String abl_pl) {

        ContentValues values = new ContentValues();
        values.put(allColumnsDeklinationsendung[1], name);
        values.put(allColumnsDeklinationsendung[2], nom_sg);
        values.put(allColumnsDeklinationsendung[3], nom_pl);
        values.put(allColumnsDeklinationsendung[4], gen_sg);
        values.put(allColumnsDeklinationsendung[5], gen_pl);
        values.put(allColumnsDeklinationsendung[6], dat_sg);
        values.put(allColumnsDeklinationsendung[7], dat_pl);
        values.put(allColumnsDeklinationsendung[8], akk_sg);
        values.put(allColumnsDeklinationsendung[9], akk_pl);
        values.put(allColumnsDeklinationsendung[10], abl_sg);
        values.put(allColumnsDeklinationsendung[11], abl_pl);

        getWritableDatabase().insert(DeklinationsendungDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Lektion' table in the database with given parameters.
     * @param titel content of the column 'titel' in the database entry
     * @param thema content of the column 'thema' in the database entry
     */
    private void addRowLektion(int lektionNr, String titel, String thema) {

        ContentValues values = new ContentValues();
        values.put(allColumnsLektion[1], lektionNr);
        values.put(allColumnsLektion[2], titel);
        values.put(allColumnsLektion[3], thema);

        getWritableDatabase().insert(LektionDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Personalendung_Präsens' table in the database with given parameters.
     * @param erste_sg  content of the column 'erste_sg' in the database entry
     * @param zweite_sg content of the column 'zweite_sg' in the database entry
     * @param dritte_sg content of the column 'dritte_sg' in the database entry
     * @param erste_pl  content of the column 'erste_pl' in the database entry
     * @param zweite_pl content of the column 'zweite_pl' in the database entry
     * @param dritte_pl content of the column 'dritte_pl' in the database entry
     */
    private void addRowPersonalendung_Praesens(String erste_sg, String zweite_sg,
                                               String dritte_sg, String erste_pl,
                                               String zweite_pl, String dritte_pl) {

        ContentValues values = new ContentValues();
        values.put(allColumnsPersonalendung_Präsens[1], erste_sg);
        values.put(allColumnsPersonalendung_Präsens[2], erste_pl);
        values.put(allColumnsPersonalendung_Präsens[3], zweite_sg);
        values.put(allColumnsPersonalendung_Präsens[4], zweite_pl);
        values.put(allColumnsPersonalendung_Präsens[5], dritte_sg);
        values.put(allColumnsPersonalendung_Präsens[6], dritte_pl);

        getWritableDatabase().insert(Personalendung_PräsensDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Präposition' table in the database with given parameters.
     * @param deutsch    content of the column 'deutsch' in the database entry
     * @param latein     content of the column 'latein' in the database entry
     * @param lektion_id foreign key: the corresponding entry from the 'Lektion' table
     */
    private void addRowPraeposition(String deutsch, String latein,
                                    int lektion_id){

        ContentValues values = new ContentValues();
        values.put(allColumnsPraeposition[1], deutsch);
        values.put(allColumnsPraeposition[2], latein);
        values.put(allColumnsPraeposition[3], 0);
        values.put(allColumnsPraeposition[4], 0);
        values.put(allColumnsPraeposition[5], lektion_id);

        getWritableDatabase().insert(PräpositionDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Sprechvokal_Präsens' table in the database with given parameters.
     * @param erste_sg  content of the column 'erste_sg' in the database entry
     * @param zweite_sg content of the column 'zweite_sg' in the database entry
     * @param dritte_sg content of the column 'dritte_sg' in the database entry
     * @param erste_pl  content of the column 'erste_pl' in the database entry
     * @param zweite_pl content of the column 'zweite_pl' in the database entry
     * @param dritte_pl content of the column 'dritte_pl' in the database entry
     */
    private void addRowSprechvokal_Praesens(String titel, String infinitiv,
                                            String erste_sg, String zweite_sg,
                                            String dritte_sg, String erste_pl,
                                            String zweite_pl, String dritte_pl) {

        ContentValues values = new ContentValues();
        values.put(allColumnsSprechvokal_Präsens[1], titel);
        values.put(allColumnsSprechvokal_Präsens[2], infinitiv);
        values.put(allColumnsSprechvokal_Präsens[3], erste_sg);
        values.put(allColumnsSprechvokal_Präsens[4], zweite_sg);
        values.put(allColumnsSprechvokal_Präsens[5], dritte_sg);
        values.put(allColumnsSprechvokal_Präsens[6], erste_pl);
        values.put(allColumnsSprechvokal_Präsens[7], zweite_pl);
        values.put(allColumnsSprechvokal_Präsens[8], dritte_pl);

        getWritableDatabase().insert(Sprechvokal_PräsensDB.FeedEntry.TABLE_NAME, null, values);

    }

    /**
     * Adds a entry to the 'Sprechvokal_Substantiv' table in the database with given parameters.
     * @param nom_sg content of the column 'nom_sg' in the database entry
     * @param nom_pl content of the column 'nom_pl' in the database entry
     * @param gen_sg content of the column 'gen_sg' in the database entry
     * @param gen_pl content of the column 'gen_pl' in the database entry
     * @param dat_sg content of the column 'dat_sg' in the database entry
     * @param dat_pl content of the column 'dat_sg' in the database entry
     * @param akk_sg content of the column 'akk_sg' in the database entry
     * @param akk_pl content of the column 'akk_pl' in the database entry
     * @param abl_sg content of the column 'abl_sg' in the database entry
     * @param abl_pl content of the column 'abl_pl' in the database entry
     */
    private void addRowSprechvokal_Substantiv(
            String nom_sg, String nom_pl,
            String gen_sg, String gen_pl,
            String dat_sg, String dat_pl,
            String akk_sg, String akk_pl,
            String abl_sg, String abl_pl) {

        ContentValues values = new ContentValues();
        values.put(allColumnsSprechvokal_Substantiv[1], nom_sg);
        values.put(allColumnsSprechvokal_Substantiv[2], nom_pl);
        values.put(allColumnsSprechvokal_Substantiv[3], gen_sg);
        values.put(allColumnsSprechvokal_Substantiv[4], gen_pl);
        values.put(allColumnsSprechvokal_Substantiv[5], dat_sg);
        values.put(allColumnsSprechvokal_Substantiv[6], dat_pl);
        values.put(allColumnsSprechvokal_Substantiv[7], akk_sg);
        values.put(allColumnsSprechvokal_Substantiv[8], akk_pl);
        values.put(allColumnsSprechvokal_Substantiv[9], abl_sg);
        values.put(allColumnsSprechvokal_Substantiv[10], abl_pl);

        getWritableDatabase().insert(Sprechvokal_SubstantivDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Sprichwort' table in the database with given parameters.
     * @param deutsch    content of the column 'deutsch' in the database entry
     * @param latein     content of the column 'latein' in the database entry
     * @param lektion_id foreign key: the corresponding entry from the 'Lektion' table
     */
    private void addRowSprichwort(String deutsch, String latein,
                                  int lektion_id){

        ContentValues values = new ContentValues();
        values.put(allColumnsSprichwort[1], deutsch);
        values.put(allColumnsSprichwort[2], latein);
        values.put(allColumnsSprichwort[3], 0);
        values.put(allColumnsSprichwort[4], 0);
        values.put(allColumnsSprichwort[5], lektion_id);

        getWritableDatabase().insert(SprichwortDB.FeedEntry.TABLE_NAME, null, values);
    }

    /**
     * Adds a entry to the 'Substantiv' table in the database with given parameters.
     * @param nom_sg_deutsch        content of the column 'nom_sg_deutsch' in the database entry
     * @param wortstamm             content of the column 'wortstamm' in the database entry
     * @param lektion_id            foreign key: the corresponding entry from the 'Lektion' table
     * @param sprechvokal_id        foreign key: the corresponding entry from the 'Sprechvokal_Substantiv' table
     * @param deklinationsendung_id foreign key: the corresponding entry from the 'Deklinationsendung' table
     */
    private void addRowSubstantiv(String nom_sg_deutsch, String wortstamm,
                                  int lektion_id, int sprechvokal_id, int deklinationsendung_id) {

        ContentValues values = new ContentValues();
        values.put(allColumnsSubstantiv[1], nom_sg_deutsch);
        values.put(allColumnsSubstantiv[2], wortstamm);
        values.put(allColumnsSubstantiv[3], 0);
        values.put(allColumnsSubstantiv[4], 0);
        values.put(allColumnsSubstantiv[5], lektion_id);
        values.put(allColumnsSubstantiv[6], sprechvokal_id);
        values.put(allColumnsSubstantiv[7], deklinationsendung_id);

        getWritableDatabase().insert(SubstantivDB.FeedEntry.TABLE_NAME, null, values);
    }
    /**
     * Adds a entry to the 'Verb' table in the database with given parameters.
     * @param infinitiv_deutsch content of the column 'infinitiv_deutsch' in the database entry
     * @param wortstamm         content of the column 'wortstamm' in the database entry
     * @param konjugation       content of the column 'konjugation' in the database entry
     * @param lektion_id        foreign key: the corresponding entry from the 'Lektion' table
     * @param personalendung_id foreign key: the corresponding entry from the 'Personalendung_Präsens' table
     * @param sprechvokal_id    foreign key: the corresponding entry from the 'Sprechvokal_Substantiv' table
     */
    private void addRowVerb(String infinitiv_deutsch, String wortstamm, String konjugation,
                            int lektion_id, int personalendung_id, int sprechvokal_id) {

        ContentValues values = new ContentValues();
        values.put(allColumnsVerb[1], infinitiv_deutsch);
        values.put(allColumnsVerb[2], wortstamm);
        values.put(allColumnsVerb[3], konjugation);
        values.put(allColumnsVerb[4], 0);
        values.put(allColumnsVerb[5], 0);
        values.put(allColumnsVerb[6], lektion_id);
        values.put(allColumnsVerb[7], personalendung_id);
        values.put(allColumnsVerb[8], sprechvokal_id);

        getWritableDatabase().insert(VerbDB.FeedEntry.TABLE_NAME, null, values);
    }

    private void addRowBeispielsatz(int subjekt_id, int praedikat_id, int genitiv_id, int dativ_id, int akkusativ_id){

        //FIXME: We currently add a placeholder vocabulary for empty spaces (_ID == -1)
        //We need to fill out all spaces in the initialisation document or find a better solution.
        //The way this is handled right now is not acceptable and cannot be released this way
        if (subjekt_id == -1) subjekt_id = 2;
        if (praedikat_id == -1) praedikat_id = 2;
        if (genitiv_id == -1) genitiv_id = 2;
        if (dativ_id == -1) dativ_id = 2;
        if (akkusativ_id == -1) akkusativ_id = 2;

        ContentValues values = new ContentValues();
        values.put(allColumnsBeispielsatz[1], subjekt_id);
        values.put(allColumnsBeispielsatz[2], praedikat_id);
        values.put(allColumnsBeispielsatz[3], genitiv_id);
        values.put(allColumnsBeispielsatz[4], dativ_id);
        values.put(allColumnsBeispielsatz[5], akkusativ_id);

        getWritableDatabase().insert(BeispielsatzDB.FeedEntry.TABLE_NAME, null, values);
    }

    private void addRowSubjunktion(String deutsch, String latein, int lektion){

        ContentValues values = new ContentValues();
        values.put(allColumnsSubjunktion[1], deutsch);
        values.put(allColumnsSubjunktion[2], latein);
        values.put(allColumnsSubjunktion[3], 0);
        values.put(allColumnsSubjunktion[4], 0);
        values.put(allColumnsSubjunktion[5], lektion);

        getWritableDatabase().insert(SubjunktionDB.FeedEntry.TABLE_NAME, null, values);
    }






    /**
     * This method is for the class 'DevAndroidDatabaseManager'
     * Remove this before release
     * @param Query
     * @return
     */
    @SuppressWarnings("all")
    @SuppressLint("all")
    public ArrayList<Cursor> getData (String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[]{"message"};
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2 = new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);

        try {
            String maxQuery = Query;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);

            //add value to cursor2
            Cursor2.addRow(new Object[]{"Success"});

            alc.set(1, Cursor2);
            if (null != c && c.getCount() > 0) {

                alc.set(0, c);
                c.moveToFirst();

                return alc;
            }
            return alc;
        } catch (SQLException sqlEx) {
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[]{"" + sqlEx.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        } catch (Exception ex) {
            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[]{"" + ex.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        }
    }

}