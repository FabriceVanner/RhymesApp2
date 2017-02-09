package rhymesapp;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import static rhymesapp.Constatics.*;
import static rhymesapp.StringsAndStuff.*;

//import static main.java.rhymesApp.IOUtils.*;
/*
automated db query time-measures
- 2 table nur mit src-wörtern und als foreign key ids von dem rhyme string eines anderen Tables
- array list oder hash table key-value mit src-wörtern und (entweder db-index foreign key oder line number einer textfile oder gleich dem jeweiligen Rhyme-string)

*/

/**
 * class to manage access to the sqlite-db
 */
public class DataBaseHelper extends SQLiteOpenHelper {

    //The Android's default system path of your application database.
    //private static String INT_STORAGE_DBFILEPATH = "/data/data/";
    //private static String INT_STORAGE_DBFILEPATH = "";
    //private static String APP_PACKAGENAME = "";
    //private static String ADD_DB_PATH = "/databases/";
    //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    //mnt/media_rw/3234-3432/
    //private static String EXT_STORAGE_DBFILEPATH = EXT_STORAGE_USER_PATH_ONE + "/" + DB_FILENAME;


    //private static String DB_FILEPATH ="";
    private static File INTERNAL_DB_FILE;
    private static InputStream EXTERNAL_DB_FILE_INPUTSTREAM;

    private static SQLiteDatabase myDataBase;
    private final Context myContext;
    private static boolean dbReadyLoaded = false;
    private static boolean hmReadyLoaded = false;
    private static boolean dbFileisLoadable = false;

    private static final String  LOG_TAG = "RA - Database";
    //private RhymesBaseActivity RA;
    /** the hashmap used to faster find the relevant db-rows*/
    private static HashMap<String, Integer> wordIndexHashMap;
    private static DataBaseHelper dataBaseHelperSingleton;
    /** used for getting random rhymes from database*/
    private static Random random;

    private static long dbEntriesTableRowCount = -1;

    public static synchronized DataBaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (dataBaseHelperSingleton == null) {
            dataBaseHelperSingleton = new DataBaseHelper(context.getApplicationContext());
        }
        return dataBaseHelperSingleton;
    }


    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application ASSETS and resources.
     *
     * @param context
     */
    private DataBaseHelper(Context context) {
        super(context, getDbFilename(), null, 1);
        this.myContext = context;
        //  this.RA = RA;
    }

    public static String getDbFilename() {
        return Constatics.DB_FILENAME;
    }


    public static boolean isEnabledHashMapPrefetch() {
        return Constatics.isEnableHashMapPrefetch();
    }

    public static boolean isDbReadyLoaded() {
        return dbReadyLoaded;
    }

    public static void setDbReadyLoaded(boolean dbReadyLoaded) {
        DataBaseHelper.dbReadyLoaded = dbReadyLoaded;
    }


    /**
     * loads Hashmap diract or via thread
     * if direct: sets loadHashMapPrefetch = true
     * if via thread: thread handler sets hm and sets setHmReadyLoaded(true) when finished
     */
    public  void loadHashMapPrefetch() {
            //Constatics.setEnableHashMapPrefetch(true);
            if(Constatics.enableHashMapThreadLoading) {
                new AsyncHMLoaderTask().execute();
                //PARALLEL THREADS:
                //   new AsyncHMLoaderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


                // ist thread schon gestarted?
                /*
                if (((Constatics.IOUtils.getMyHashMapThread().getState().compareTo(Thread.State.NEW)) ==0)) {
                    Constatics.IOUtils.sethandler();
                    Constatics.IOUtils.getMyHashMapThread().start();
                }
                */
                return;
            }else{
                this.setWordIndexHashMap(Constatics.IOUtils.deserializeHashMap());
                setHmReadyLoaded(true);
            }
    }

    /**
     * deserializes the hashmap in background to memory
     */
    private class AsyncHMLoaderTask extends AsyncTask<Void, Void, Bundle> {
        @Override
        protected Bundle doInBackground(Void... params) {
            Log.d(LOG_TAG, "AsyncHMLoaderTask doInBackground():  starting to load HM...");
            HashMap hm = Constatics.IOUtils.deserializeHashMap();
            Bundle bundle = new Bundle();
            bundle.putBoolean("hmReadyLoaded",true);
            bundle.putSerializable("hashMap",hm);
//            Log.d(LOG_TAG, "AsyncHMLoaderTask doInBackground():  just loaded HM");
            return bundle;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {
            HashMap<String, Integer> hm= (HashMap<String, Integer> )bundle.getSerializable("hashMap");
            Constatics.dataBaseHelper.setWordIndexHashMap(hm);
            Constatics.dataBaseHelper.setHmReadyLoaded(true);
            Log.d(LOG_TAG, "AsyncHMLoaderTask onPostExecute():  ...just loaded HM");
        }
    }

    private HashMap<String, Integer> getWordIndexHashMap() {
        return wordIndexHashMap;
    }

    public  void setWordIndexHashMap(HashMap<String, Integer> wordIndexHashMap) {
        DataBaseHelper.wordIndexHashMap = wordIndexHashMap;
    }

    public static boolean isHmReadyLoaded() {
        return hmReadyLoaded;
    }

    /**
     * sets the boolean and posts a toast on gui
     * @param hmReadyLoaded
     */
    public  void setHmReadyLoaded(boolean hmReadyLoaded) {
        this.hmReadyLoaded = hmReadyLoaded;
        if(hmReadyLoaded){
            Toast.makeText(myContext, "setHmReadyLoaded() "+ HM_READY, Toast.LENGTH_SHORT).show();
            Log.v(LOG_TAG,  "setHmReadyLoaded() "+ HM_READY);
        }
    }

    protected void onSaveInstanceState(Bundle state) {
        if (getWordIndexHashMap() != null) {
            //state.putSerializable("myDatabase",myDataBase);
            state.putBoolean("isEnableHashMapPrefetch", Constatics.isEnableHashMapPrefetch());
            /*
            if(Constatics.isEnableHashMapPrefetch()) {
                state.putSerializable("wordIndexHashMap", getWordIndexHashMap());
            }
            */
        }

        /**DIe beiden sind ja nach jeden activity circle false, oder? */
        //state.putBoolean("hmReadyLoaded", isHmReadyLoaded());
        //state.putBoolean("dbReadyLoaded", dbReadyLoaded);

        state.putBoolean("dbFileisLoadable", dbFileisLoadable);
        state.putSerializable("INTERNAL_DB_FILE", INTERNAL_DB_FILE);
        state.putLong("dbEntriesTableRowCount",dbEntriesTableRowCount);
    }


    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Constatics.setEnableHashMapPrefetch(savedInstanceState.getBoolean("isEnableHashMapPrefetch"));


        if(Constatics.isEnableHashMapPrefetch()) {
            loadHashMapPrefetch();
            //setWordIndexHashMap((HashMap) savedInstanceState.getSerializable("wordIndexHashMap"));
        }

        /**DIe beiden sind ja nach jeden activity circle false, oder? */
       // dbReadyLoaded = savedInstanceState.getBoolean("dbReadyLoaded", false);
       // setHmReadyLoaded(savedInstanceState.getBoolean("hmReadyLoaded", false));
        dbFileisLoadable = savedInstanceState.getBoolean("dbFileisLoadable", false);
        INTERNAL_DB_FILE = (File) savedInstanceState.getSerializable("INTERNAL_DB_FILE");
        dbEntriesTableRowCount = savedInstanceState.getLong("dbEntriesTableRowCount",-1);
        //startTime = (Calendar) savedInstanceState.getSerializable("starttime");
    }


    public String getTableNames() {
        Cursor c = myDataBase.rawQuery(SQLITE_GET_TABLES, null);
        String str = "File-Size = (MB)" + Integer.toString(Math.round(INTERNAL_DB_FILE.length()) / (1024 * 1024)) + "  - ";
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                //System.out.println("########################################\n");
                str += ("Table: " + c.getString(0) + " ");

                c.moveToNext();
            }
        }
        long cnt = DatabaseUtils.queryNumEntries(myDataBase, "words");

        /*
        String countQuery = "SELECT  * FROM words";
        Cursor cursor = myDataBase.rawQuery(countQuery, null);
        int cnt = cursor.getCount();
        */
        str += " row count = " + cnt;
        Log.v(LOG_TAG, str);
        Toast.makeText(myContext, "getTableNames() " + str, Toast.LENGTH_SHORT).show();
        return str;
    }

    public InputStream getExternalDBStreamOfFile(){
        return EXTERNAL_DB_FILE_INPUTSTREAM;
    }

    public void setExternalDBStreamOfFile(InputStream is){
        EXTERNAL_DB_FILE_INPUTSTREAM = is;
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     *
     * @param forceDBCopy
     * @param copyIfDifferentSize
     */
    public void setUpInternalDataBase(boolean forceDBCopy, boolean copyIfDifferentSize) throws IOException {
        if(dbFileisLoadable)return;

        /**TODO: er ruft in getSetDBFile() wie auch in filesAreEqualSize() 2 mal hintereinander die getOutputFile() methode auf*/
        if(getInternalDBFile()==null) setInternalDBFile(Constatics.IOUtils.getOutputFile(dbDstLocation, getDbFilename()));
        //check of file to be copied is db-file
        if (!getInternalDBFile().exists()){
            Log.v(LOG_TAG,INTERNAL_DB_NOT_EXISTS + dbDstLocation.toString());
            copyDb();
            return;
        }else{
            if (!isDataBase(getInternalDBFile())){
                copyDb();
                return;
            }
        }
        if(getExternalDBStreamOfFile()==null) setExternalDBStreamOfFile(Constatics.IOUtils.getInputStream(dbSrcLocation,getDbFilename()));

        if (forceDBCopy){
            copyDb();
            return;
        }
        if(copyIfDifferentSize &&!Constatics.IOUtils.filesAreEqualSize(getExternalDBStreamOfFile(), getInternalDBFile(), Constatics.acceptableFileSizeDifference)){
            copyDb();
            return;
        }
        return;
    }

    private void copyDb() throws IOException {
        this.getReadableDatabase();//TODO: what for?
        Constatics.IOUtils.copyStreams(getExternalDBStreamOfFile(), new FileOutputStream(getInternalDBFile()));
    }



        /**
         * Check if the database already exist to avoid re-copying the file eachEntry time you open the application.
         *
         * @return true if it exists, false if it doesn't
         */
    /** checks if given file can be opened by SQLite*/
    private boolean isDataBase(File file) {
        if (file == null) return false;
        if(!file.exists()) return false;
        SQLiteDatabase checkDB = null;
        //TODO.
        try {
            checkDB = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            Log.e(LOG_TAG,"isDataBaseFile(): "+file.getAbsolutePath()+" is not a db file");
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }

    public void setInternalDBFile(File file)throws IOException{
        INTERNAL_DB_FILE = file;
    }
    public File getInternalDBFile(){
        if (INTERNAL_DB_FILE != null) return INTERNAL_DB_FILE;
        return null;
    }



    /**
     * Opens the database
     * sets dbFileisLoadable = true
     * loads hashmap if option is enabled
     * @throws SQLException
     */
    public void openDataBase() throws SQLException {
        myDataBase = SQLiteDatabase.openDatabase(getInternalDBFile().getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        dbFileisLoadable = true;
        setDbReadyLoaded(true);
        if (Constatics.isEnableHashMapPrefetch()){
            this.loadHashMapPrefetch();
        }else{
            this.setWordIndexHashMap(null);
        }
    }

    @Override
    public synchronized void close() {

//TODO müssen irgendwann geschlossen werden
    //    if (myDataBase != null)myDataBase.close();

    //    super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**checks if db is ready and performs a random rhymes query */
    public WordRhymesPair getRandWordRhymesPair(){
        if (isDbReadyLoaded()) {
                return getRandomRhymesDirectFromDB();
        } else {
            String mess = "getRandWordRhymesPair(): DB or HM not loaded or copied to internal mem yet";
            Constatics.IOUtils.postToastMessageToGuiThread(mess);
            Log.e(LOG_TAG,mess);
            return null;
        }
    }

    private long getDBEntriesTableRowCount(){
        if (dbEntriesTableRowCount==-1){
            dbEntriesTableRowCount = DatabaseUtils.queryNumEntries(myDataBase, TABLE_WORDS);
        }
        return dbEntriesTableRowCount;
    }

    /**
     * performs a random rhymes query
     * @return
     */
    private WordRhymesPair getRandomRhymesDirectFromDB(){
        if (random==null){            random = new Random();        }
        /*TODO: fliegt evtl. um die Ohren wenn long zu groß ist... */
        int randomRowNr = random.nextInt((int)getDBEntriesTableRowCount());
        String[] columns = new String[]{Constatics.COLUMN_WORD,Constatics.COLUMN_RHYMES};
        /**TODO: falls ID nicht linear vergeben ist kann es sein dass eine row angefordert wird, die es nicht gibt!!*/
        /**TODO: besser nicht nach ID sondern einer db internen Row-numerierung querien */
        Cursor cursor = myDataBase.query(TABLE_WORDS,columns,Constatics.COLUMN_ID+ " = "+randomRowNr+";",null,null,null,null);
        return getStringFromCursor(cursor);
    }



    // Add your public helper methods to access and get content fromIndex the database.
    // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
    // to you to create adapters for your views.


    /**
     * queries the rhymes for the given word
     * decides via the i-var booleans whether to use hashmap first or query the db directly
     * @param word
     * @return
     */
    public String getRhymes(String word) {
        if (isDbReadyLoaded() && (!isEnabledHashMapPrefetch() || (isEnabledHashMapPrefetch() && isHmReadyLoaded()))) {
            if (isEnabledHashMapPrefetch()) {
                return getRhymesViaHashMap(word,Constatics.ignoreCase);
            } else {
                return getRhymesDirectFromDB(word, Constatics.useFuzzySearch,Constatics.ignoreCase);
            }
        } else {
            String mess = "DB or HM not loaded or copied to internal mem yet";
            Constatics.IOUtils.postToastMessageToGuiThread(mess);
            Log.e(LOG_TAG,"getRhymes(): "+mess);
            return "";
        }
    }


    /**
     *
     * @param word
     * @param useFuzzySearch will use sql comparator "like"
     * @param ignoreCase ignores cases
     * @return
     */
    private String getRhymesDirectFromDB(String word, boolean useFuzzySearch, boolean ignoreCase) {
        String sqlComparator ="=";
        if (useFuzzySearch) sqlComparator = "LIKE";
        String[] rhymeColumn = new String[]{Constatics.COLUMN_WORD,Constatics.COLUMN_RHYMES};
        //SELECT * FROM ... WHERE name = 'someone' COLLATE NOCASE   --> case ignore
        Cursor cursor = myDataBase.query(TABLE_WORDS, rhymeColumn, Constatics.COLUMN_WORD +" "+ sqlComparator+" "+"'" + word.replaceAll("'", "''") + "' ;", null, null, null, null, null);
        //synchronized dataquery:         //CursorLoader cursorLoader = new CursorLoader(); LoaderManager


        //  getTableNames();
        // Cursor cursor = myDataBase.query(MySQLiteHelper.TABLE_WORDS,
        //        allColumns, null, null, null, null, null);
        // while (!cursor.isAfterLast()) {
        //!cursor.isAfterLast()
        //      ||
        //cursor.moveToNext();
        //}
        // make sure to close the cursor

        String rhymes = getStringFromCursor(cursor).getRhymes();
        if ( ignoreCase && rhymes.equals("")){
            Log.v(LOG_TAG, "...trying other case of "+word);
            String newWord;
            if (Character.isUpperCase(word.charAt(0))) {
                newWord = word.toLowerCase();
            } else {
                newWord = Character.toUpperCase(word.charAt(0))+word.substring(1,word.length());
            }
            newWord = newWord.replaceAll("'", "''");
            cursor = myDataBase.query(TABLE_WORDS, rhymeColumn, Constatics.COLUMN_WORD +" "+ sqlComparator+" "+"'" +  newWord+ "' ;", null, null, null, null, null);
            rhymes = getStringFromCursor( cursor).getRhymes();
        }
        String msg;
        if (rhymes==""){
            msg =  "Not in DB (in any Case): " + word;
            Constatics.IOUtils.postToastMessageToGuiThread(msg);
        }else{
           msg= "getRhymesDirectFromDB(): found "+ word+" in DB";
        }
        Log.i(LOG_TAG,msg);

        return rhymes;

    }

    /**
     * returns the first String result (at column-index 0) of this cursor
     * @param cursor
     * @return
     */
    private WordRhymesPair getStringFromCursor(Cursor cursor){
        String rhymes = "";
        String word ="";
        cursor.moveToFirst();
        if ((cursor.getCount() > 0)) {
            word =cursor.getString(0);
            rhymes = cursor.getString(1);
        }else{
            String mess =  "getStringFromCursor(): Not in DB(with this case-beginning)";
            //Toast.makeText(myContext, mess, Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG,mess);
            rhymes = "";
        }


        cursor.close();
        return new WordRhymesPair(word,rhymes);
    }





    private String getRhymesViaHashMap(String word, boolean ignoreCase) {
        int index = 4;
        boolean found= false;
        String newWord=word;
        if (getWordIndexHashMap() == null) {
            Toast.makeText(myContext, "HashMap is null! ", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Hashmap: Can't look me up, I'm null!");
            return "";
        }
        if (getWordIndexHashMap().containsKey(newWord)) {
            found = true;
        } else if(ignoreCase) {
            if (Character.isUpperCase(word.charAt(0))) {
                newWord = word.toLowerCase();
                if (getWordIndexHashMap().containsKey(newWord)) {
                    found = true;
                }
            } else {
                newWord = Character.toUpperCase(word.charAt(0))+word.substring(1,word.length());
                if (getWordIndexHashMap().containsKey(newWord)) {
                    found = true;
                }
            }
        }


        if(!found) {
            String mess = "Not in HM(DB): " + word;
            Constatics.IOUtils.postToastMessageToGuiThread(mess);
           // Toast.makeText(myContext,mess , Toast.LENGTH_SHORT).show();
            return "";
        }

        index = getWordIndexHashMap().get(newWord);
        String[] rhymeColumn = new String[]{Constatics.COLUMN_WORD,Constatics.COLUMN_RHYMES};
        Cursor cursor = myDataBase.query(TABLE_WORDS, rhymeColumn, Constatics.COLUMN_ID + "=" + index, null, null, null, null, null);
        String rhymes = getStringFromCursor(cursor).getRhymes();

        if (rhymes==""){
            String mess =  "Not in DB: " + word;
            Constatics.IOUtils.postToastMessageToGuiThread(mess);
            return "";
        }
        Log.i(LOG_TAG,"getRhymesViaHashMap(): found "+ word+" in DB");


        return rhymes;
    }


}