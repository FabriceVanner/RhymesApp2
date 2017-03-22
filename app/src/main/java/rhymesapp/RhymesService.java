package rhymesapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.rhymesapp.R;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

import static rhymesapp.StringsAndStuff.ERR_NOT_OPEN_DB;

public class RhymesService extends Service implements TextToSpeech.OnInitListener  {




    //http://blog.nkdroidsolutions.com/android-foreground-service-example-tutorial/
    public Vector<String> rhymeResults;
    private Constatics constatics;
    public static final String BROADCAST_ACTION = "rhymesapp";
    PendingIntent pendingIntent;
    public static  boolean IS_SERVICE_RUNNING = false;
    private static RhymesService rhymesService;
    //private static BaseActToServiceStorage baseActToServiceStorage;
    private static final String LOG_TAG = "RhymesService";

    static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";

    static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";




    LocalBroadcastManager broadcaster;
    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        rhymesService = this;
        constatics = Constatics.getInstance(this);
        initDataProvider();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this.getApplicationContext();
        //      return Service.START_NOT_STICKY;

        if (intent.getAction().equals(Constatics.ACTION.STARTFOREGROUND_ACTION)) {
            Toast.makeText(this,"Start Service",Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Received Start Foreground Intent ");


            Intent notificationIntent = new Intent(this, RhymesBaseActivity.class);
            notificationIntent.setAction(Constatics.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            RemoteViews notificationView = new RemoteViews(this.getPackageName(), R.layout.notification);

            // And now, building and attaching the Play button.
              Intent buttonPlayIntent = new Intent(this, NotificationPlayButtonHandler.class);
              buttonPlayIntent.putExtra("action", "togglePlay");

              PendingIntent buttonPlayPendingIntent = pendingIntent.getBroadcast(this, 0, buttonPlayIntent, 0);
              notificationView.setOnClickPendingIntent(R.id.notification_button_play, buttonPlayPendingIntent);

            // And now, building and attaching the Skip button.
            Intent buttonSkipIntent = new Intent(this, NotificationSkipButtonHandler.class);
            buttonSkipIntent.putExtra("action", "skip");

            PendingIntent buttonSkipPendingIntent = pendingIntent.getBroadcast(this, 0, buttonSkipIntent, 0);
            notificationView.setOnClickPendingIntent(R.id.notification_button_skip, buttonSkipPendingIntent);

            // And now, building and attaching the Skip button.
            Intent buttonPrevIntent = new Intent(this, NotificationPrevButtonHandler.class);
            buttonPrevIntent.putExtra("action", "prev");

            PendingIntent buttonPrevPendingIntent = pendingIntent.getBroadcast(this, 0, buttonPrevIntent, 0);
            notificationView.setOnClickPendingIntent(R.id.notification_button_prev, buttonPrevPendingIntent);

            // And now, building and attaching the Close button.
            Intent buttonCloseIntent = new Intent(this, NotificationCloseButtonHandler.class);
            buttonCloseIntent.putExtra("action", "close");

            PendingIntent buttonClosePendingIntent = pendingIntent.getBroadcast(this, 0, buttonCloseIntent, 0);
            notificationView.setOnClickPendingIntent(R.id.notification_button_close, buttonClosePendingIntent);



            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("nkDroid Music Player")
                    .setTicker("nkDroid Music Player")
                    .setContentText("nkDroid Music")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContent(notificationView)
                    .setOngoing(true).build();



            startForeground(Constatics.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);
        }


        else if (intent.getAction().equals(Constatics.ACTION.STOPFOREGROUND_ACTION)) {
            Toast.makeText(this,"Stop Service",Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "In onDestroy");
    }

    /**
     * Called when user clicks the "play/pause" button on the on-going system Notification.
     */
    public static class NotificationPlayButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            Toast.makeText(context,"Play Clicked "+ action,Toast.LENGTH_SHORT).show();
            rhymesService.toggleTimerHandler();
        }
    }

    /**
     * Called when user clicks the "skip" button on the on-going system Notification.
     */
    public static class NotificationSkipButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context,"Next Clicked",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when user clicks the "previous" button on the on-going system Notification.
     */
    public static class NotificationPrevButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context,"Previous Clicked",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when user clicks the "close" button on the on-going system Notification.
     */
    public static class NotificationCloseButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context,"Close Clicked",Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Task used to run the rhyme-queries in background
     */
    private class AsyncRhymesQueryTask extends AsyncTask<AsynchRhymesQueryParamWrapper, Void, AsynchRhymesQueryParamWrapper> {
        @Override
        protected AsynchRhymesQueryParamWrapper doInBackground(AsynchRhymesQueryParamWrapper... query) {
            Log.d(LOG_TAG, "AsyncRhymesQueryTask doInBackground(): just run rhymes query with Nr.: " + query[0].nr + " and word " + query[0].word);
            query[0].rhymes = runRhymesQuery(query[0].word);
            return query[0];
        }


        @Override
        protected void onPostExecute(AsynchRhymesQueryParamWrapper result) {
            rhymeResults.add(result.rhymes);
            if (result.nr == 1) {
                //prepareAndSendColoredTextView(outputTextView, result.rhymes);
                ///broadcastTextViewTextToGui("coloredOutputTextView" ,result.rhymes );
            }
            Log.d(LOG_TAG, "AsyncRhymesQueryTask onPostExecute():  just added results of query " + result.nr + "( " + result.word + " ) to rhymeResults-Arraylist");
        }
    }


    public void sendResult(String message) {
        Intent intent = new Intent(COPA_RESULT);
        if(message != null)
            intent.putExtra(COPA_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }
      //   SERVICE TO ACTIVITY COMMUNICATION
    private void broadcastTextViewTextToGui(String type, String text){
        //Intent textViewToGuiIntent = new Intent(context, RhymesBaseActivity.ServiceTextToGuiHandler.class);
        //PendingIntent textViewToGuiPendingIntent;

        Intent textViewToGuiIntent = new Intent(COPA_RESULT);
        textViewToGuiIntent.putExtra( "TYPE",type);
        textViewToGuiIntent.putExtra( "TEXT",text);
        broadcaster.sendBroadcast(textViewToGuiIntent);
        /*
        textViewToGuiPendingIntent = pendingIntent.getBroadcast(context, 0, textViewToGuiIntent, 0);
        // for service broadcast
        try {
            // Perform the operation associated with our pendingIntent
            textViewToGuiPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        */
    }

    private static Handler timerHandler = new Handler();
    public static int autoRandomSpeedinMS = 4000;
    public static Context context;
    public static boolean enableAutoRandom=false;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            findRandomWordPair();
            timerHandler.postDelayed(this, autoRandomSpeedinMS);
        }
    };



    private void findRandomWordPair() {
        //broadcastTextViewTextToGui("coloredOutputTextView","dies ist TExt vom Service");
        //Toast.makeText(context, "findRandomWordPair()", Toast.LENGTH_SHORT).show();
        new AsyncRandomRhymesQuery().execute(0);
    }

    public void toggleTimerHandler() {
        enableAutoRandom = !enableAutoRandom;
        if(enableAutoRandom){
            enableAutoRandom = true;
            timerHandler.postDelayed(timerRunnable, 4000);}
        else {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
    private boolean enableTextToSpeech;

    private class AsyncRandomRhymesQuery extends AsyncTask<Integer, Void, WordRhymesPair> {
        @Override
        protected rhymesapp.WordRhymesPair doInBackground(Integer... query) {
            // Log.d(LOG_TAG, "AsyncRhymesQueryTask doInBackground(): just run rhymes query with Nr.: "+query[0].nr + " and word "+query[0].word  );
            return Constatics.dataBaseHelper.getRandWordRhymesPair();
        }


        @Override
        protected void onPostExecute(rhymesapp.WordRhymesPair wordRhymesPair) {
            super.onPostExecute(wordRhymesPair);
            //    randomRhymesQuery = wordRhymesPair;
            showRandomWordRhymesPair(wordRhymesPair);
            if (enableTextToSpeech) {
                if (textToSpeechEngine == null) {
                    loadTextToSpeech();
                    onInit(TextToSpeech.SUCCESS);
                }
                speak(wordRhymesPair.getWord());
            }
        }

    }

    TextToSpeech textToSpeechEngine;

    @Override
    public void onInit(int status) {
        //Log.d(&Speech&, &OnInit - Status [&+status+&]&);

        if (status == TextToSpeech.SUCCESS) {
            //  Log.d(&Speech&, &Success!&);
            textToSpeechEngine.setLanguage(Locale.GERMAN);
        }
    }

    private void speak(String text) {
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }


    private void loadTextToSpeech() {
        textToSpeechEngine = new TextToSpeech(this.getApplicationContext(), this);
    }

    private void showRandomWordRhymesPair(WordRhymesPair wordRhymesPair) {
        if (wordRhymesPair == null) return;
        broadcastTextViewTextToGui("inputTextView", wordRhymesPair.getWord());
        broadcastTextViewTextToGui("coloredOutputTextView", wordRhymesPair.getRhymes());
    }




    private String runRhymesQuery(String word) {
        if (word == null || word.length() == 0) {
            Log.v(LOG_TAG, "runRhymesQuery: word == null or == \"\" ");
            return "";
        }
        String rhymes = "";
        rhymes = constatics.dataBaseHelper.getRhymes(word);

        //  rhymes.replaceAll("\\\\n",System.getProperty("line.separator"));
        //rhymes=rhymes.replaceAll("\\n","\n");
        return rhymes;//+\\\n sdfsdfsdf";
    }





    private void initDataProvider() {
        //myDbHelper = getInstance(this);
        /* TODO: Uncomment*/

        try {
            constatics.dataBaseHelper.setUpInternalDataBase(Constatics.forceCopyOfDBFile, Constatics.copyDBFileIfDifferentSize);
        } catch (IOException ioe) {

            String mess = "";
            if (ioe.getCause() != null) {
                mess = ioe.getCause().getMessage();
            }
            mess += " at Location " + ioe.getMessage();
            //    Log.e(LOG_TAG,ioe.getCause().getMessage());
            Log.e(LOG_TAG, mess);
            Toast.makeText(this, ERR_NOT_OPEN_DB + mess, Toast.LENGTH_SHORT).show();
            return;
        }
        constatics.dataBaseHelper.openDataBase();


        //myDbHelper.getTableNames();
    }
    public void setSystemNotification(){
        /*


        A foreground service is a service that should have the same priority as an active activity and therefore should not be killed by the Android system, even if the system is low on memory. A foreground service must provide a notification for the status bar, which is placed under the "Ongoing" heading, which means that the notification cannot be dismissed unless the service is either stopped or removed from the foreground.
        Notification notification = new Notification(R.drawable.icon, "bla",
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, RhymesBaseActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "notificationTitle","notification_message", pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
*/
    }



    private final IBinder mBinder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        RhymesService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RhymesService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** method for clients */
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }
}



