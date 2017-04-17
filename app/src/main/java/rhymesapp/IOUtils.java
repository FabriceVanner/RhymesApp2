package rhymesapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.util.HashMap;

import static android.os.Environment.getExternalStorageDirectory;
import static rhymesapp.Constatics.*;
import static rhymesapp.StringsAndStuff.ERR_ON_COPY_STREAMS;

//import static main.java.rhymesApp.DataBaseHelper.getSetDBFile;

/**
 * Created by entwickler01 on 13.09.16.
 */
public class IOUtils {

    private static Context myContext;


    private static String LOG_TAG = "RA - IOUtils";
    private static IOUtils IOUtilsSingleton;
    /** used to post messages to gui*/
    private Handler handler;


    public static synchronized IOUtils getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (IOUtilsSingleton == null) {
            IOUtilsSingleton = new IOUtils(context.getApplicationContext());
        }
        return IOUtilsSingleton;
    }

    private IOUtils(Context context){
        this.myContext = context;
        //myDataBaseHelper = DataBaseHelper.getInstance(myContext);
        if (Constatics.enableHashMapThreadLoading) {

        }
        sethandler();

    }


    protected void onRestoreInstanceState(Bundle savedInstanceState) {

    }
    /**
     *
     * compares file-sizes
     * @param accuracy
     * @return returns false if one or both are null
     * @throws IOException
     */
    public static boolean filesAreEqualSize(InputStream src, File dst, float accuracy)throws IOException{
        if(src==null||dst==null) return false;
        float diff = Math.abs(src.available()-dst.length());
        if (diff< accuracy*dst.length())return true;

        return false;
    }


    /**
     * checks if storage is readable
     * @throws IOException
     */
    public static void externalStorageReadable()throws IOException{
        // achtung: getExternalStorageState bezieht sich evtl nur emulated verzeichnisse
        if (Environment.getExternalStorageState() == null) {
            throw new IOException("Environment.getExternalStorageState()== null");
        }
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e(LOG_TAG, "Error: Could not read external storage");
            throw new IOException("Error: Could not read external storage");
        }

    }

    /**
     * needed to access raw-ressource-files
     * @return
     */
    private static String getPackageName(){
        PackageManager m = myContext.getPackageManager();
        String packageName = myContext.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(packageName, 0);
            return p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Error Package name not found ", e);
        }
        return null;
    }








    /**
     * performs readability tests on the directoy first
     * returns a file-obj. (might me a newly created file?)
     *  TODO: gerade wird weder lese(input) noch schreib-berechtigung(wäre wichtig für output) der datei selbst geprüft
     * @param folderPath
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File checkAndGetFile(String folderPath, String fileName) throws IOException {
        String filepath = folderPath + "/" + fileName;
        File directory = new File(folderPath);
        if (!directory.exists()) {
            String errormsg="Folder doesnt exit: " + directory.getAbsolutePath();
            Log.e(LOG_TAG,"checkAndGetFile()"+ errormsg );
            throw new IOException(errormsg);
        }
        if (!directory.canRead()) {
            String errormsg ="Could not read Folder: " + directory.getAbsolutePath();
            Log.e(LOG_TAG,"checkAndGetFile()"+ errormsg );
            throw new IOException(errormsg);
        }
        File file = new File(filepath);
        /*
        if (!file.exists()) {
            String errormsg ="Error: file doesnt exit: " + file.getAbsolutePath();
            Log.e("yourtag",errormsg );
            throw new IOException(errormsg);
        }
        if (!file.canRead()) {
            String errormsg ="Error: Could not read file" + file.getAbsolutePath();
            Log.e("yourtag",errormsg);
            throw new IOException(errormsg);
        }
*/
                            /*
                            // if no directory exists, create new directory
                            if ((!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
                                directory.mkdir();
                                Log.w("yourtag", "CREATED FOLDER " + USER_FOLDERNAME_SD + " on Device SD");
                                throw new IOException()
                                break;
                            }
                            */

        return file;
    }

    /** handler to post message to the gui thread*/
    public void sethandler(){
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String toastMessage = bundle.getString("toastMessage");
                if (toastMessage!= null){
                    Toast.makeText(myContext, toastMessage, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }
    public void postToastMessageToGuiThread(String toastMessage){
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("toastMessage",toastMessage);
        msg.setData(bundle);
        msg.sendToTarget();
    }



/*THreadhandler
    public void sethandler(){
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if (bundle.getBoolean("hmReadyLoaded")) {
                    HashMap<String, Integer> hm= (HashMap<String, Integer> )bundle.getSerializable("hashMap");
                    Constatics.dataBaseHelper.setWordIndexHashMap(hm);
                    Constatics.dataBaseHelper.setHmReadyLoaded(true);
                    Log.d(LOG_TAG, "handler: HM ready loaded ");
                }
            }
        };
    }
*/


    public  HashMap<String, Integer> deserializeHashMap() {
        HashMap<String, Integer>  hm=null;
        try {
            //FileInputStream fis = new FileInputStream();
            //= new FileInputStream(EXT_STORAGE_USER_PATH_ONE + "/" + clientArgs.SERIALIZED_HASHMAP_FILENAME);
            InputStream is = getInputStream(Filelocation.ASSETS, Constatics.SERIALIZED_HASHMAP_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(is);
            hm = (HashMap<String, Integer>) ois.readObject();
            ois.close();
            is.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
            return null;
        }
        Log.d(LOG_TAG, "deserializeHashMap() HM ready loaded");
        return hm;
    }


    /**TODO: works, but still needed?   */
    private Thread myHashMapThread = new Thread (new Runnable(){
        @Override
        public void run() {
            HashMap hm = deserializeHashMap();
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putBoolean("hmReadyLoaded",true);
            bundle.putSerializable("hashMap",hm);
            msg.setData(bundle);
            msg.sendToTarget();
            Log.d(LOG_TAG, "myHashMapThread:  just sent message to target");


        }
    });

    public Thread getMyHashMapThread() {
        return myHashMapThread;
    }



    /**ASSETS = ASSETS folder,
     * res-raw = ressource raw folder,
     * internal package = app-packacke,
     * INTERNAL_DATA = Environment.getDataDirectory()
     * USER_FOLDER_INTERNAL = see INT_STORAGE_USER_PATH
     * INTERNAL_DATABASES =
     * */
    public enum Filelocation {
        ASSETS, RES_RAW, EXT_STORAGE_USER, INTERNAL_PACKAGE, USER_FOLDER_INTERNAL, INTERNAL_DATA, INTERNAL_DATABASES
    }

    /**
     * gets an input-stream from a specific loacation (ASSETS-folder, raw-ressource, Ext-Storage), performs also a readabilty check if EXT_STORAGE is chosen
     * @param srcLocation
     * @param fileName
     * @return
     * @throws IOException
     */
    public static InputStream getInputStream(Filelocation srcLocation, String fileName) throws IOException {
        InputStream myInput = null;
        Log.i(LOG_TAG,"getInputStream(): Looking for File "+fileName +" at "+srcLocation.toString() );
        try {
            switch (srcLocation) {
                case ASSETS:
                    //Open your local db as the input stream
                    myInput = myContext.getAssets().open(fileName);
                    break;
                case RES_RAW:
                    myInput = myContext.getResources().openRawResource(myContext.getResources().getIdentifier(fileName.substring(0, fileName.indexOf(".") - 1), "raw", getPackageName()));
                    break;
                case EXT_STORAGE_USER:
                    externalStorageReadable();
                    if(EXT_STORAGE_USER_PATH_ONE ==null|| EXT_STORAGE_USER_PATH_ONE.equals(""))
                        EXT_STORAGE_USER_PATH_ONE = getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS;//TODO: Slash hier richtig?
                    File file;
                    try {
                        file = checkAndGetFile(EXT_STORAGE_USER_PATH_ONE, fileName);
                    }catch (IOException ioe){
                        file = checkAndGetFile(EXT_STORAGE_USER_PATH_TWO,fileName);
                    }
                    //      Toast.makeText(myContext, "taking file as input: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    myInput = (new FileInputStream(file));
                    break;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG,"getInputStream(): " +srcLocation.toString());
            throw new StorageLocationException(srcLocation.toString(),ioe);
        }
        return myInput;
    }



    /**
     *
     *
     *
     * */

    /**
     * performs a check and returns a file-obj for enums: INTERNAL PACKAGE, USER FOLDER INTERNAL, INTERNAL DATA, INTERNAL DATABASES
     *
     *
     * @param dstLocation
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File getDstFileObj(Filelocation dstLocation, String fileName) throws IOException {
        File file = null;
        Log.i(LOG_TAG,"getDstFileObj(): Looking for File "+fileName +" at "+dstLocation.toString() );
        switch (dstLocation) {
            case INTERNAL_PACKAGE:
                file = checkAndGetFile(getPackageName(), fileName);
                break;
            case USER_FOLDER_INTERNAL:
                if(INT_STORAGE_USER_PATH==null||INT_STORAGE_USER_PATH.equals(""))INT_STORAGE_USER_PATH=myContext.getFilesDir().getAbsolutePath()+ Environment.DIRECTORY_DOWNLOADS;
                file = checkAndGetFile(INT_STORAGE_USER_PATH, fileName);
                break;
            case INTERNAL_DATA:
                file = checkAndGetFile(Environment.getDataDirectory().getAbsolutePath(),fileName);
                break;
            case INTERNAL_DATABASES:
                file =  (myContext.getDatabasePath(fileName));
                checkAndGetFile(file.getParent(),fileName);
                break;
        }
     //   Toast.makeText(myContext, "getDstFileObj() Using: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        return file;

    }


    /**
     * Copies your database fromIndex your local ASSETS-folder to the just created empty database in the
     * system folder, fromIndex where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    public static void copyStreams(InputStream dbSrcLocation, OutputStream dbDSTLocation) throws IOException {
        try{
            //transfer bytes fromIndex the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            int i = 0;
            Log.v(LOG_TAG, "COPYING Streams:");
            while ((length = dbSrcLocation.read(buffer)) > 0) {
                i++;
                if (i % 10000 == 0) Log.v(LOG_TAG, "... "+i+"*"+1024 + " = "+ (i*1024)+" bytes");
                dbDSTLocation.write(buffer, 0, length);
            }
            //Close the streams
            dbDSTLocation.flush();
            dbDSTLocation.close();
            dbSrcLocation.close();
        } catch (IOException ioe){
            System.out.println(ioe.toString());
            Log.e(LOG_TAG,ERR_ON_COPY_STREAMS );
            throw new IOException(ERR_ON_COPY_STREAMS);
        }
    }






}
