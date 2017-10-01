package rhymesapp;

import android.content.Context;
import android.os.Environment;

import static rhymesapp.IOUtils.Filelocation.EXT_STORAGE_USER;
import static rhymesapp.IOUtils.Filelocation.USER_FOLDER_INTERNAL;

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
    /** the location where the database-file is initially hosted by the user*/
    public static IOUtils.Filelocation dbSrcLocation = EXT_STORAGE_USER;
    /** the location where the database-file gets copied to internally*/
    public static IOUtils.Filelocation dbDstLocation = USER_FOLDER_INTERNAL;//INTERNAL_DATABASES;

    /**TODO: still needs to be changed for each Device!!*/
    public static String INT_STORAGE_USER_PATH= "/storage/emulated/0/" + Environment.DIRECTORY_DOWNLOADS ;//  /data/user/0

    /**TODO: still needs to be changed for each Device!!*/
    //public static String EXT_STORAGE_USER_PATH_ONE  = "/storage/3234-3432/Download";
    public static String EXT_STORAGE_USER_PATH_TWO = "/storage/283A-8C79/Download";
    public static String EXT_STORAGE_USER_PATH_ONE;


    public static String dbURL = "http://i.imgur.com/RRUe0Mo.png";


    /** the difference the external and internal (copied) db may show without recopying the db to internal memory*/
    static final float acceptableFileSizeDifference=0.01f;
    static boolean copyDBFileIfDifferentSize = false;

    /** copy the db from external to internal no matter what*/
    static boolean forceCopyOfDBFile = false;

    /** expected to be in the user-download folder on the device*/
    static final String DB_FILENAME = "rhymes.db";

    /**HashMap is expected to be in the ASSETS folder! */
    static final String SERIALIZED_HASHMAP_FILENAME = "wordIndexHM.ser";

    /** SEARCH */

    static  final boolean ENABLEHASHMAPPREFETCHDEFAULT = false;
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
    static  final boolean ENABLESPEECHRECOGNITIONDEFAULT = true;



    /* GUI SETTINGS: DEFFAULT */
    static final RhymesService.QueryType QUERYTYPEDEFAULT = RhymesService.QueryType.RHYME;
    static final boolean ENABLETEXTTOSPEECHDEFAULT = false;
    static final int AUTORANDOMSPEEDINMSDEFAULT = 6000;
    static final boolean ENABLEWAKELOCKDEFAULT = false;
    static final int TEXTFIELDSFONTSIZEDEFAULT =26;


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
        //dataBaseHelper= DataBaseHelper.getInstance(rhymesBaseActivity.getApplicationContext());
        IOUtils = IOUtils.getInstance(rhymesBaseActivity.getApplicationContext());
        guiUtils = GuiUtils.getInstance(rhymesBaseActivity);
        //  this.RA = RA;
    }


    private Constatics(Context context) {
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


//####################################################################################################################
    //SERVICE:
    /* ACHTUNG: jede hier definierte action muss auch im LocalbroadcastManager im IntentFilter mit .addAction(  ) definiert werden, sonst filtert der ddie weg*/
public interface ACTION {
    public static String MAIN_ACTION = "rhymesapp.alertdialog.action.main";
    public static String COLOREDOUTPUTTEXTVIEW_ACTION = "rhymesapp.action.showcoloredtextoutput";
    public static String INPUTTEXTVIEW_ACTION = "rhymesapp.action.inputtextview";
    public static String TOGGLEPLAY_ACTION = "rhymesapp.action.toggleplay";
    public static String ADDTORHYMERESULTVECTOR_ACTION = "rhymesapp.action.addtorhymeresultvector";
    public static String CLICKABLEWORDSTOINPUTTEXTVIEw_ACTION = "rhymesapp.action.clickablewordstoinputtextview";
    public static String EMPTYRHYMERESULTSVECTOR_ACTION = "rhymesapp.action.emptyrhymeresultsvector";
    public static String SHOWDOWNLOADORCOPYDIALOG_ACTION = "rhymesapp.action.showdownloadorcopydialog";
    public static String UPDATEDOWNCOPYPROGRESS_ACTION = "rhymesapp.action.updatedowncopyprogress";
    public static String SHOWDOWNLOADDIALOG_ACTION = "rhymesapp.action.showdownloaddialog";
    public static String OUTPUTTEXTVIEW_ACTION = "rhymesapp.action.outputtextview";
    public static String CLOSEAPP_ACTION = "rhymesapp.action.closeapp";
    public static String UPDATEAUTORANDOMBUTTON_ACTION = "rhymesapp.action.toggleautorandom";
    public static String RANDOMQUERY_ACTION = "rhymesapp.action.randomquery";
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
 output field bei erneuter QUERYTYPEDEFAULT wieder nach oben setzen
 zoom listener auf outputfield  http://blog.stevepark.org/2012/06/android-java-code-for-pinch-zoom-event.html
 fuzzy-search sqlite LIKE und in Hashmap?


 * */