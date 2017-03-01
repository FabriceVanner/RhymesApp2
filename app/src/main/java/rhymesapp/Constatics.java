package rhymesapp;

import android.content.Context;
import android.os.Bundle;

import static rhymesapp.IOUtils.Filelocation.EXT_STORAGE_USER;
import static rhymesapp.IOUtils.Filelocation.INTERNAL_DATABASES;

/**
 * Created by Fabrice Vanner on 14.09.2016.
 *
 * Home of constants and static vars
 * and
 * static classes
 *
 */
public class Constatics {

/** DB */
    public static final String TABLE_WORDS = "words";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WORD = "word";
    // Database creation sql statement
    static final String DATABASE_CREATE = "create table "
            + TABLE_WORDS + "( " + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_WORD
            + " text not null);";
    public static final String COLUMN_RHYMES = "rhymes";
    private static final String[] allColumns = {COLUMN_ID,
            COLUMN_WORD, COLUMN_RHYMES};

    /** FILE IO */
    /** the location where the database-file is initially saved*/
    public static IOUtils.Filelocation dbSrcLocation = EXT_STORAGE_USER;
    /** the location where the database-file gets copied to internally*/
    public static IOUtils.Filelocation dbDstLocation = INTERNAL_DATABASES;

    /**TODO: still needs to be changed for each Device!!*/
    public static String INT_STORAGE_USER_PATH; //= "/storage/emulated/0/" + Environment.DIRECTORY_DOWNLOADS ;

    /**TODO: still needs to be changed for each Device!!*/
    //public static String EXT_STORAGE_USER_PATH_ONE  = "/storage/3234-3432/Download";
    public static String EXT_STORAGE_USER_PATH_TWO = "/storage/283A-8C79/Download";
    public static String EXT_STORAGE_USER_PATH_ONE;




    /** the difference the external and internal (copied) db may show without recopying the db to internal memory*/
    static final float acceptableFileSizeDifference=0.01f;
    static boolean copyDBFileIfDifferentSize = true;

    /** copy the db from external to internal no matter what*/
    static boolean forceCopyOfDBFile = false;

    /** expected to be in the user-download folder on the device*/
    static final String DB_FILENAME = "rhymes.db";

    /**HashMap is expected to be in the ASSETS folder! */
    static final String SERIALIZED_HASHMAP_FILENAME = "wordIndexHM.ser";

    /** SEARCH */

    static  final boolean enableHashMapPrefetchDefault = false;
    static  final boolean enableHashMapThreadLoading = true;
    /** the setting changed by gui-elements / the user */
    private static  boolean enableHashMapPrefetch = false;

    /** whether to query the database fuzzy - won't work with hashmap search*/
    static  final boolean useFuzzySearch = false;
    /** ignore case-differences when querying the db*/
    static final boolean ignoreCase=true;

    /** SPEECH RECOG*/

    /** recognize english speech as well*/
    static  final boolean addEngSpeechRecog= false;
    static  final boolean enableSpeechRecognitionDefault = true;

    static final boolean enableTextToSpeechDefault = false;
    static final int autoRandomSpeedinMSDefault = 6000;
    static final boolean enableWakeLockDefault = false;
    /** SINGLETONS*/
    public static IOUtils IOUtils;
    public static GuiUtils guiUtils;
    public static DataBaseHelper dataBaseHelper;
    private static Constatics constaticsSingleton;


    public static synchronized Constatics getInstance(Context context) {
        if (constaticsSingleton == null) {
            constaticsSingleton = new Constatics(context.getApplicationContext());
        }
        return constaticsSingleton;
    }

    public static synchronized Constatics getInstance(RhymesBaseActivity rhymesBaseActivity) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (constaticsSingleton == null) {
            constaticsSingleton = new Constatics(rhymesBaseActivity);
        }
        return constaticsSingleton;
    }

    private Constatics(RhymesBaseActivity rhymesBaseActivity) {
        dataBaseHelper= DataBaseHelper.getInstance(rhymesBaseActivity.getApplicationContext());
        IOUtils = IOUtils.getInstance(rhymesBaseActivity.getApplicationContext());
        guiUtils = GuiUtils.getInstance(rhymesBaseActivity);
        //  this.RA = RA;
    }


    private Constatics(Context context) {
        dataBaseHelper= DataBaseHelper.getInstance(context);
        IOUtils = IOUtils.getInstance(context);
        guiUtils = GuiUtils.getInstance(context);
        //  this.RA = RA;
    }

    public static boolean isEnableHashMapPrefetch() {
        return enableHashMapPrefetch;
    }

    public static void setEnableHashMapPrefetch(boolean enableHashMapPrefetch) {
        Constatics.enableHashMapPrefetch = enableHashMapPrefetch;
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        dataBaseHelper.onRestoreInstanceState(savedInstanceState);
        IOUtils.onRestoreInstanceState(savedInstanceState);
    }
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        dataBaseHelper.onSaveInstanceState(savedInstanceState);
        //IOUtils.onSaveInstanceState(savedInstanceState);
    }
//####################################################################################################################
    //SERVICE:
public interface ACTION {
    public static String MAIN_ACTION = "rhymesapp.alertdialog.action.main";
    public static String STARTFOREGROUND_ACTION = "rhymesapp.alertdialog.action.startforeground";
    public static String STOPFOREGROUND_ACTION = "rhymesapp.alertdialog.action.stopforeground";
}

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }


//####################################################################################################################


}

/**


Inputfield durch anclicken (oder knopf) editable --> keyboard
Activity Circle: Objects wie Databasehelper als Serializable oder bundle saven?




########### UNWICHTIGER:
 enum: prefer local speech recognition over online
 Zeit messung - automat. querytest
 reime als serialized string array ( binary in db) ablegen lässt sich danach evtl einfacher im display darstellen
 Output Field in zwei Columns teilen
 output field bei erneuter query wieder nach oben setzen
 zoom listener auf outputfield  http://blog.stevepark.org/2012/06/android-java-code-for-pinch-zoom-event.html
 fuzzy-search sqlite LIKE und in Hashmap?


 * */