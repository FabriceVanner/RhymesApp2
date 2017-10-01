package rhymesapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.rhymesapp.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;

import static newDevelopments.HTMLDataProvider.scrapeAssociationSite;
import static rhymesapp.Constatics.ACTION.*;
import static rhymesapp.RhymesService.AutoRandomType.QUERYWORD;
import static rhymesapp.RhymesService.AutoRandomType.QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT;
import static rhymesapp.StringsAndStuff.ERR_NOT_OPEN_DB;
//http://codetheory.in/understanding-android-started-bound-services/

/**
 * This class takes care of all background - not to be interupted activities:
 */
public class RhymesService extends Service implements TextToSpeech.OnInitListener, RecognitionListener {
    private static final String LOG_TAG = "RhymesService";
    public static Context context;
    public static boolean IS_FOREGROUND_SERVICE_RUNNING = false;

    /**
     * if the app has been freshly started and is not just beeing resumed from background
     */
    public static boolean isFreshlyStarted = true;
    private static RhymesService rhymesService;

    // CLASS OBJECTS
    public DataBaseHelper dataBaseHelper;
    private GuiUtils guiUtils;

    // options
    private boolean enableSpeechRecognition = true;


    /**
     * to store the results retourned by the async rhyme-queries (for the speak-recog. results)
     */
    public Vector<String> rhymeResults;


    // LIFECYCLE:

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate(): ");
        super.onCreate();
        context = this.getApplicationContext();
        broadcaster = LocalBroadcastManager.getInstance(this);
        rhymesService = this;
        //DataBaseHelper.getInstance(rhymesBaseActivity.getApplicationContext());
        dataBaseHelper = DataBaseHelper.getInstance(getApplicationContext(), this);
        guiUtils = GuiUtils.getInstance(getApplicationContext());
        initDataProvider();
        rhymeResults = new Vector<>();

        if (this.enableSpeechRecognition) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            if (Constatics.addEngSpeechRecog) {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                        "en");
            }
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    this.getPackageName());
            //recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        }

        if (enableTextToSpeech) {
            textToSpeechEngine = new TextToSpeech(this, this);
        }
        //TODO: hier oder in OnStartCommand?:
        if (!IS_FOREGROUND_SERVICE_RUNNING) {
            Log.d(LOG_TAG, "onCreate(): !IS_FOREGROUND_SERVICE_RUNNING: ");
            initForegroundService();
        }

    }

    protected void onPause() {
        Log.i(LOG_TAG, "onPause()");
        //boolean isBound = false;
/*
        boolean speechServiceIsBound = getApplicationContext().bindService( new Intent(getApplicationContext(), SpeechRecognizer.class), Context.BIND_AUTO_CREATE );
        if (speechServiceIsBound)
            getApplicationContext().unbindService(serviceConnection);
*/

        if (speech != null) {
            //speak.destroy();
        }
        dataBaseHelper.close();

    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        //Canceling notifications: //TODO: gut und nötig?
        //      NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
        IS_FOREGROUND_SERVICE_RUNNING = false;
        Log.d(LOG_TAG, "dataBaseHelper.close()");
        dataBaseHelper.close();
    }

// LOCAL FOREGROUND SERVICE & Notification TRAY BUTTONS

    //http://blog.nkdroidsolutions.com/android-foreground-service-example-tutorial/

    Notification foregroundServiceNotification;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder mNotifyBuilder;


    public void initForegroundService() {
        // TODO: no notification on Moto E  https://stackoverflow.com/questions/46111860/oreo-foreground-service-does-not-show-foreground-notification
        Log.i(LOG_TAG, "initForegroundService(): Received Start Foreground Intent ");

        Intent notificationIntent = new Intent(this, RhymesBaseActivity.class);
        notificationIntent.setAction(Constatics.ACTION.MAIN_ACTION);
        //TODO: die folgende zeile ist für das verhalten beim click auf die notification verantwortlich: soll keine neue activity starten!
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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


        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// Sets an ID for the notification, so it can be updated
        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("RhymesApp")
                .setTicker("RhymesApp")
                .setContentText("RhymesApp")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContent(notificationView)
                //.setCustomBigContentView(notificationView)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
        //.setStyle(new Notification.BigTextStyle().bigText(longText))
        ;
        foregroundServiceNotification = mNotifyBuilder.build();
        foregroundServiceNotification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        /**
         Is your service a started service or a bound service? I had the same issue with a bound service,
         but starting the service before binding it allowed me to call startForeground(int, foregroundServiceNotification)
         with and have the foregroundServiceNotification show up.

         */
        Log.d(LOG_TAG, "startForeground():");
        startForeground(Constatics.NOTIFICATION_ID.FOREGROUND_SERVICE, foregroundServiceNotification);
        IS_FOREGROUND_SERVICE_RUNNING = true;
        //  startForeground(1,foregroundServiceNotification); http://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification

        checkLastInstanceStateAndEventuallyStartOrStopAutoRandom();

    }


    public void buildNotification() {

    }

    /**
     * This is the method that can be called to update the Notification
     */
    private void updateNotification() {


        //foregroundServiceNotification.
        //     findViewById(R.id.notification_button_play)).setImageResource();
        mNotificationManager.notify(101, foregroundServiceNotification);
    }

    /**
     *
     */
    public void stopForegroundService() {
        Log.i(LOG_TAG, "stopForegourndService(): Received Stop Foreground Intent");
        Log.d(LOG_TAG, "StopForeground()");
        stopForeground(true);
        Log.d(LOG_TAG, "stopSelf()");
        stopSelf();
    }

    /**
     * onStartCommand() is called every time a client starts the service using startService(Intent intent). This means that onStartCommand() can get called multiple times. You should do the things in this method that are needed each time a client requests something from your service. This depends a lot on what your service does and how it communicates with the clients (and vice-versa).
     * <p>
     * If you don't implement onStartCommand() then you won't be able to get any information from the Intent that the client passes to onStartCommand() and your service might not be able to do any useful work.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand()");
        context = this.getApplicationContext();
        //      return Service.START_NOT_STICKY;

        //if (intent.getAction().equals(Constatics.ACTION.STARTFOREGROUND_ACTION)) {
        //     initForegroundService();
        //} else if (intent.getAction().equals(Constatics.ACTION.STOPFOREGROUND_ACTION)) {
        //TODO: von Activity in onDestroy oder so, aufrufen lassen   stopForegroundService();
        // }
        return START_STICKY;
    }

    public void checkLastInstanceStateAndEventuallyStartOrStopAutoRandom() {
        Log.d(LOG_TAG, "checkLastInstanceStateAndEventuallyStartOrStopAutoRandom()");
        if (isDbReadyLoaded()) {
            if (enableAutoRandom) {
                runTimerHandler1();
                mNotifyBuilder.mNotification.contentView.setImageViewResource(R.id.notification_button_play, android.R.drawable.ic_media_pause);
            } else {
                stopTimerHandler();
                mNotifyBuilder.mNotification.contentView.setImageViewResource(R.id.notification_button_play, android.R.drawable.ic_media_play);
            }

            //// Local Service Binding Communication
            //if (rhymesServiceIsBound) {

            // Call a method from the LocalService.                // However, if this call were something that might hang, then this request should                // occur in a separate thread to avoid slowing down the activity performance.
            //}
                /*
                Intent buttonPlayIntent = new Intent(context, RhymesService.NotificationPlayButtonHandler.class);
                PendingIntent buttonPlayPendingIntent;
                    //for intent broadcast to service
                    buttonPlayIntent.putExtra("action", "togglePlay");

                buttonPlayPendingIntent = pendingIntent.getBroadcast(context, 0, buttonPlayIntent, 0);
                */
                /*
                //for intent broadcast to service
                try {
                    // Perform the operation associated with our pendingIntent
                    buttonPlayPendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                */
        } else {
            if (enableAutoRandom) {
                Log.d(LOG_TAG, "Autorandom is true; but can't init or load Database: switching autorandom off");
                enableAutoRandom = false;
                mNotifyBuilder.mNotification.contentView.setImageViewResource(R.id.notification_button_play, android.R.drawable.ic_media_play);
                rhymesService.broadcastCommandToBaseActivity(UPDATEAUTORANDOMBUTTON_ACTION, "");
            }
        }
        mNotificationManager.notify(101, mNotifyBuilder.build());
    }

    public void toggleAutoRandom() {
        Log.d(LOG_TAG, "toggleAutoRandom()");
        enableAutoRandom = !enableAutoRandom;
        checkLastInstanceStateAndEventuallyStartOrStopAutoRandom();

    }

    /**
     * Called when user clicks the "play/pause" button on the on-going system Notification.
     */
    public static class NotificationPlayButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "RhymesService: NotificationPlayButtonHandler: onReceive():");
            String action = intent.getStringExtra("action");
            // Toast.makeText(context, "Play Clicked " + action, Toast.LENGTH_SHORT).show();
            //rhymesService.broadcastCommandToBaseActivity("toggleNotification_button_play_image","");
            //foregroundServiceNotification

            //mNotifyBuilder.mNotification.contentView.setTextViewText(R.id.notification_text_artist,wordRhymesPair.getWord());
            //mNotificationManager.notify(101,mNotifyBuilder.build());

            //rhymesService.toggleTimerHandler();
            // making sure that both are on synch ?!            rhymesService.enableTextToSpeech = rhymesService.enableAutoRandom;
            rhymesService.toggleAutoRandom();
            rhymesService.broadcastCommandToBaseActivity(UPDATEAUTORANDOMBUTTON_ACTION, "");
            //rhymesService.broadcastCommandToBaseActivity(UPDATEAUTORANDOMBUTTON_ACTION,"");
        }
    }

    /**
     * Called when user clicks the "skip" button on the on-going system Notification.
     */
    public static class NotificationSkipButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rhymesService.broadcastCommandToBaseActivity(RANDOMQUERY_ACTION, "");
            Toast.makeText(context, "Next Clicked", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when user clicks the "previous" button on the on-going system Notification.
     */
    public static class NotificationPrevButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Previous Clicked", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when user clicks the "close" button on the on-going system Notification.
     */
    public static class NotificationCloseButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Close Clicked sending stop intent", Toast.LENGTH_SHORT).show();


            rhymesService.broadcastCommandToBaseActivity(CLOSEAPP_ACTION, "");
            Intent stopIntent = new Intent(context.getApplicationContext(), RhymesService.class);
            rhymesService.stopService(stopIntent);
            rhymesService.stopSelf();

            //TODO: hide foregroundServiceNotification
        }
    }

// QUERIES

    public enum QueryType {RHYME, ASSOCIATION}

    ;
    public QueryType queryType = Constatics.QUERYTYPEDEFAULT;

    private String runRhymesQuery(String word) {
        if (word == null || word.length() == 0) {
            Log.v(LOG_TAG, "runRhymesQuery: word == null or == \"\" ");
            return "";
        }
        String rhymes = "";
        rhymes = dataBaseHelper.getRhymes(word);

        //  queryResult.replaceAll("\\\\n",System.getProperty("line.separator"));
        //queryResult=queryResult.replaceAll("\\n","\n");
        return rhymes;//+\\\n sdfsdfsdf";
    }

    /**
     * looks up every element in the database by creating: AsyncRhymesQueryTasks
     *
     * @param words
     */
    public void asyncWordQuery(ArrayList<String> words) {
        for (int i = 0; i < words.size(); i++) {
            //String match = words.get(i); queryResult = runRhymesQuery(match);
            new AsyncWordQuery().execute(new AsynchWordQueryParamWrapper(i + 1, words.get(i), QueryType.RHYME));
            //PARALLEL THREADS:
            //new AsyncWordQuery().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new AsynchWordQueryParamWrapper(i+1,words.get(i)));
        }
    }

    /**
     * Task used to run the rhyme-queries in background
     * <p>
     * fills rhymeResult-array. when finishhed sets display
     */
    private class AsyncWordQuery extends AsyncTask<AsynchWordQueryParamWrapper, Void, AsynchWordQueryParamWrapper> {
        @Override
        protected AsynchWordQueryParamWrapper doInBackground(AsynchWordQueryParamWrapper... query) {
            Log.d(LOG_TAG, "AsyncWordQuery doInBackground(): just run queryResult QUERYTYPEDEFAULT with Nr.: " + query[0].nr + " and word " + query[0].word);
            query[0].queryResult = runRhymesQuery(query[0].word);
            return query[0];
        }

        @Override
        protected void onPostExecute(AsynchWordQueryParamWrapper result) {
            //rhymeResults.add(result.queryResult);
            if (result.nr == 1) {
                broadcastCommandToBaseActivity(COLOREDOUTPUTTEXTVIEW_ACTION, result.queryResult);
            }
            //broadcastCommandToBaseActivity("addToRhymeResultVector", result.queryResult);
            rhymeResults.add(result.queryResult);
            Log.d(LOG_TAG, "AsyncWordQuery onPostExecute():  just added results of QUERYTYPEDEFAULT " + result.nr + "( " + result.word + " ) to rhymeResults-Arraylist");
        }
    }


    private boolean asyncRandomWordQueryisRunning = false;

    //TODO: ACHTUNG : 01.10.17 habe Integer auf QueryType geändert
    private class AsyncRandomWordQuery extends AsyncTask<QueryType, Void, WordPair> {
        @Override
        protected WordPair doInBackground(QueryType... queryType) {
            Log.d(LOG_TAG, "AsyncWordQuery: doInBackground():");
            asyncRandomWordQueryisRunning = true;
            // Log.d(LOG_TAG, "AsyncWordQuery doInBackground(): just run queryResult QUERYTYPEDEFAULT with Nr.: "+QUERYTYPEDEFAULT[0].nr + " and word "+QUERYTYPEDEFAULT[0].word  );
            if (queryType[0] == QueryType.RHYME) {
                return dataBaseHelper.getRandWordRhymesPair();
            } else if (queryType[0] == QueryType.ASSOCIATION) {

                //TODO: not really effective: gets Rhymes of rhymes-Database just to use the random word + random rhymesdatabase-words not necessarily exist in association-website-databsase
                String word = dataBaseHelper.getRandWordRhymesPair().getWord();
                return scrapeAssociationSite(word);
            }


            //TODO: REMOVE if extended:
            return dataBaseHelper.getRandWordRhymesPair();
        }

        @Override
        protected void onPostExecute(WordPair wordPair) {
            super.onPostExecute(wordPair);
            //    randomRhymesQuery = wordPair;
            Log.d(LOG_TAG, "AsyncWordQuery: OnPostExecute(): " + wordPair.getWord());
            if (autoRandomType == QUERYWORD) {
                outputResultToUser(wordPair);
            } else if (autoRandomType == QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT) {
                stopTimerHandler();
                outputResultToUser(wordPair);
                String resultWords = wordPair.getResultWords();


                randomLinearQueryResultWordList = resultWords.split("\n");
                randomLinearQueryResultWordPair = wordPair;

                runTimerHandler2();
            }
            asyncRandomWordQueryisRunning = false;
        }

        /*
        @Override
        protected WordPair onPostExecute(WordPair result) {
            return result;
            //Log.d(LOG_TAG, "AsyncWordQuery onPostExecute():  just added results of QUERYTYPEDEFAULT "+ result.nr + "( "+result.word +" ) to rhymeResults-Arraylist");
        }
        */
    }

    private void outputResultToUser(WordPair wordPair) {
        showWordPairOnGui(wordPair);
        sendRandomColoredWordToNotificationTextfield(wordPair.getWord());
        if (enableTextToSpeech) {
            outputResultAudioToUser(wordPair.getWord());
        }
    }

    private void outputResultAudioToUser(String word) {
        if (textToSpeechEngine == null) {
            loadTextToSpeech();
            onInit(TextToSpeech.SUCCESS);
        }
        speak(word);
    }


    private void sendRandomColoredWordToNotificationTextfield(String word) {
        //todo: http://stackoverflow.com/questions/14885368/update-text-of-notification-not-entire-notification

        //mNotifyBuilder.setContentText(wordPair.getWord());
        mNotifyBuilder.mNotification.contentView.setTextColor(R.id.notification_text_artist, guiUtils.getRandomColor());
        mNotifyBuilder.mNotification.contentView.setTextViewText(R.id.notification_text_artist, word);
        mNotificationManager.notify(101, mNotifyBuilder.build());
    }


// DB Database

    public boolean isDbReadyLoaded() {
        return dataBaseHelper.isDbReadyLoaded();
    }

    public void initDataProvider() {
        //myDbHelper = getInstance(this);
        /* TODO: Uncomment*/
        boolean cont = false;
        try {
            dataBaseHelper.setUpInternalDataBase();
        } catch (IOException ioe) {
            exceptionsToErrormessages(ioe);
            broadcastCommandToBaseActivity(OUTPUTTEXTVIEW_ACTION, "No loadable DB On Storage");
            broadcastCommandToBaseActivity(SHOWDOWNLOADORCOPYDIALOG_ACTION, "");
            return;
        }
        dataBaseHelper.openDataBase();
        //myDbHelper.getTableNames();
    }


    protected void exceptionsToErrormessages(Exception ioe) {
        String mess = "";
        if (ioe.getCause() != null) {
            mess = ioe.getCause().getMessage();
        }
        mess += " at Location " + ioe.getMessage();
        //    Log.e(LOG_TAG,ioe.getCause().getMessage());
        Log.e(LOG_TAG, mess);
        Toast.makeText(this, ERR_NOT_OPEN_DB + mess, Toast.LENGTH_LONG).show();
        return;
    }

    // AUTO RANDOM FUNCTION:
    private static Handler timerHandler = new Handler();
    private static Handler timerHandler2 = new Handler();
    public static int autoRandomSpeedinMS = 4000;
    public static boolean enableAutoRandom = false;
    public static boolean enableAutoRandom_LinearWithinQueryResults = false;

    /**
     * QUERYWORD = automatically randomquery through the possible key-words of rhymes database
     * QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT = first randomquery a key-word and then query from top till down through the results-list
     */
    public enum AutoRandomType {
        QUERYWORD, QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT
    }

    public AutoRandomType autoRandomType = QUERYWORD;

    /**
     * 1.
     */
    private Runnable timerRunnableRandomQuery = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "timerRunnableRandomQuery: run() ");
            //if (!(autoRandomType == QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT && asyncRandomWordQueryisRunning))
            randomQuery();
            /*

                //TODO: nicht correct: er wird am ende jedes durchlaufes der einzelnen wörter im result
                // asynchron evtl. noch einen Intervall drannhängen und dadurch unter umständen die länge des wartens verdoppeln
                timerHandler.postDelayed(this, autoRandomSpeedinMS);
            } else {
            */

            runTimerHandler1();
            // }
        }
    };
    private WordPair randomLinearQueryResultWordPair = null;
    /**limit of words to be iterated through */
    public int linearAutoRandomWordNr = 10;
    private String[] randomLinearQueryResultWordList;
    int randomLinearQueryResultWordListIndex = 0;

    /**
     *
     */
    private Runnable timerRunnableRandomLinearQuery = new Runnable() {
        @Override
        public void run() {
            if (randomLinearQueryResultWordListIndex == randomLinearQueryResultWordList.length || (!(enableAutoRandom && autoRandomType == QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT))){
                //randomLinearQueryResultWordListIndex==linearAutoRandomWordNr-1
                randomLinearQueryResultWordListIndex=0;
                stopTimerHandler2();
                runTimerHandler1();
                return;
            }
            // break condition in case settings in GUI are changed
            Log.d(LOG_TAG, "timerRunnableRandomLinearQuery: run(): randomLinearQueryResultWordListIndex = " + randomLinearQueryResultWordListIndex + " = " + randomLinearQueryResultWordList[randomLinearQueryResultWordListIndex]);
            if (randomLinearQueryResultWordPair == null) return;
            randomLinearQueryResultWordPair.setWord(randomLinearQueryResultWordList[randomLinearQueryResultWordListIndex]);
            broadcastCommandToBaseActivity(INPUTTEXTVIEW_ACTION, randomLinearQueryResultWordPair.getWord());
            sendRandomColoredWordToNotificationTextfield(randomLinearQueryResultWordPair.getWord());
            if (enableTextToSpeech) {
                outputResultAudioToUser(randomLinearQueryResultWordPair.getWord());
            }
            randomLinearQueryResultWordListIndex++;
            runTimerHandler2();
        }
    };


    public void randomQuery() {
        if (isDbReadyLoaded()) {
            //Toast.makeText(context, "randomQuery()", Toast.LENGTH_SHORT).show();
            new AsyncRandomWordQuery().execute(this.queryType);
        } else {
            Log.d(LOG_TAG, "randomQuery(): Cant't query: db is not ready loaded");
            Toast.makeText(context, "Cant't query: db is not ready loaded", Toast.LENGTH_SHORT).show();
        }
    }

    public void linearQuery() {

    }

    /**
     * Starts and Stops the auto-random timer loop
     */
    public void toggleTimerHandler() {
        enableAutoRandom = !enableAutoRandom;
        if (enableAutoRandom) {
            enableAutoRandom = true;
            timerHandler.postDelayed(timerRunnableRandomQuery, 4000);
        } else {
            timerHandler.removeCallbacks(timerRunnableRandomQuery);
        }
    }


    public void runTimerHandler1() {

        Log.d(LOG_TAG, "runTimerHandler1");
        timerHandler.postDelayed(timerRunnableRandomQuery, autoRandomSpeedinMS);
    }

    public void stopTimerHandler() {
        Log.d(LOG_TAG, "stopTimerHandler");
        timerHandler.removeCallbacks(timerRunnableRandomQuery);
    }

    public void runTimerHandler2() {
        Log.d(LOG_TAG, "runTimerHandler2");
        timerHandler2.postDelayed(timerRunnableRandomLinearQuery, autoRandomSpeedinMS);
    }

    public void stopTimerHandler2() {
        Log.d(LOG_TAG, "stopTimerHandler2");
        timerHandler2.removeCallbacks(timerRunnableRandomLinearQuery);
    }


//   SERVICE / ACTIVITY COMMUNICATION

    public static final String BROADCAST_ACTION = "rhymesapp";
    PendingIntent pendingIntent;
    LocalBroadcastManager broadcaster;

/*
    public void sendResult(String message) {
        Intent intent = new Intent();
        if (message != null)
            intent.putExtra(COPA_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }
*/
    //TODO RENAME ME, If you define your own actions, be sure to include your app's package name as a prefix, as shown in the following example:


    //TODO service to activity ALTERNATIVE? http://stackoverflow.com/questions/23586031/calling-activity-class-method-from-service-class
    public void broadcastCommandToBaseActivity(String action, String text) {

        //Intent textViewToGuiIntent = new Intent(context, RhymesBaseActivity.ServiceTextToGuiHandler.class);
        //PendingIntent textViewToGuiPendingIntent;
        //Intent textViewToGuiIntent = new Intent(this,RhymesBaseActivity.class);
        Intent textViewToGuiIntent = new Intent();
        textViewToGuiIntent.setAction(action);
        textViewToGuiIntent.putExtra("TEXT", text);
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

    private void showWordPairOnGui(WordPair wordRhymesPair) {
        if (wordRhymesPair == null) return;
        broadcastCommandToBaseActivity(INPUTTEXTVIEW_ACTION, wordRhymesPair.getWord());
        broadcastCommandToBaseActivity(COLOREDOUTPUTTEXTVIEW_ACTION, wordRhymesPair.getResultWords());
    }


// BINDING SERVICE TO ACTIVITY:

    private final IBinder mBinder = new LocalBinder();

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
        Log.d(LOG_TAG, "onBind()");
        return mBinder;
    }


// TEXT TO SPEECH:

    public boolean enableTextToSpeech;

    public void toggleTextToSpeech() {
        enableTextToSpeech = !enableTextToSpeech;
    }

    private void setSpeech() {
        float pitch = 1.0f;
        float speed = 1.0f;
        textToSpeechEngine.setPitch((float) pitch);
        textToSpeechEngine.setSpeechRate((float) speed);
        //textToSpeechEngine.speak(editText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }

    TextToSpeech textToSpeechEngine;

    private void speak(String text) {
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void loadTextToSpeech() {
        textToSpeechEngine = new TextToSpeech(this.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        //Log.d(&Speech&, &OnInit - Status [&+status+&]&);

        if (status == TextToSpeech.SUCCESS) {
            //  Log.d(&Speech&, &Success!&);
            textToSpeechEngine.setLanguage(Locale.GERMAN);
        }
    }


// VOICE RECOGNITION:

    private boolean speechRecognitionIsRunning = false;
    private Intent recognizerIntent;
    private SpeechRecognizer speech = null;

    public void toggleVoiceRecognition() {
        if (!speechRecognitionIsRunning) speech.startListening(recognizerIntent);
        else speech.stopListening();

        //  if (speechRecognitionIsRunning) onResults2(null);
        speechRecognitionIsRunning = !speechRecognitionIsRunning;

    }

    /**
     * used for peak-meter of audio recogn.
     *
     * @param rmsdB
     */
    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);

        //TODO: mit FRAGMENT ERSETZEN:
        //recvolumeProgrBar.setProgress((int) rmsdB);
    }

    public static String getVoiceRecognErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error fromIndex server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speak input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");

        //TODO: mit fragments in activity ersetzen
        //recvolumeProgrBar.setIndeterminate(false);
        //recvolumeProgrBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        //TODO: mit fragments in activity ersetzen
        //recvolumeProgrBar.setIndeterminate(true);
        //recvolumeProgrBar.setVisibility(View.INVISIBLE);
        //voiceRecogButton.setChecked(false);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getVoiceRecognErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);

        //TODO: mit fragments in activity  ersetzen
        //outputTextView.setText(errorMessage);
        //  voiceRecogButton.setChecked(false);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
        // Toast.makeText(RhymesBaseActivity.this, "called onReadyForSpeech()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> voiceRecogSpeechMatches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        voiceRecogSpeechMatches = guiUtils.prepareSpeechMatches(voiceRecogSpeechMatches);
        String text = guiUtils.concatStringListItems(voiceRecogSpeechMatches, ", ");

        //guiUtils.setClickableWordsInTextView(inputTextView, str, guiUtils.getDelimiterIndexes(str, ", "));        //inputTextView.setText(text);
        broadcastCommandToBaseActivity(CLICKABLEWORDSTOINPUTTEXTVIEw_ACTION, text);

        //emptyRhymeResultsArray();
        broadcastCommandToBaseActivity(EMPTYRHYMERESULTSVECTOR_ACTION, "");

        asyncWordQuery(voiceRecogSpeechMatches);
        //System.getProperty("line.separator"));  stringVar.replaceAll("\\\\n", "\\\n"); make sure your \n is in "\n" for it to work.
        //prepareAndSendTextView(queryResult);
    }


}



