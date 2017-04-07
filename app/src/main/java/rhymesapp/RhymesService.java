package rhymesapp;

import android.app.Notification;
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

import static rhymesapp.StringsAndStuff.ERR_NOT_OPEN_DB;

public class RhymesService extends Service implements TextToSpeech.OnInitListener, RecognitionListener {


    private static final String LOG_TAG = "RhymesService";
    public static Context context;
    public static boolean IS_SERVICE_RUNNING = false;
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
        super.onCreate();
        context = this.getApplicationContext();
        broadcaster = LocalBroadcastManager.getInstance(this);
        rhymesService = this;
        //DataBaseHelper.getInstance(rhymesBaseActivity.getApplicationContext());
        dataBaseHelper = DataBaseHelper.getInstance(getApplicationContext(),this);
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
        initForegroundService();

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
        super.onDestroy();
        Log.i(LOG_TAG, "In onDestroy");
    }

// LOCAL FOREGROUND SERVICE & TRAY BOTTOMS
    //http://blog.nkdroidsolutions.com/android-foreground-service-example-tutorial/

    public void initForegroundService() {
        Toast.makeText(this, "Start Service", Toast.LENGTH_SHORT).show();
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
                .setContentTitle("RhymesApp")
                .setTicker("RhymesApp")
                .setContentText("RhymesApp")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContent(notificationView)
                .setOngoing(true).build();


        startForeground(Constatics.NOTIFICATION_ID.FOREGROUND_SERVICE,
                notification);
    }

    public void stopForegroundService() {
        Toast.makeText(this, "Stop Service", Toast.LENGTH_SHORT).show();
        Log.i(LOG_TAG, "Received Stop Foreground Intent");
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this.getApplicationContext();
        //      return Service.START_NOT_STICKY;

        if (intent.getAction().equals(Constatics.ACTION.STARTFOREGROUND_ACTION)) {
            initForegroundService();
        } else if (intent.getAction().equals(Constatics.ACTION.STOPFOREGROUND_ACTION)) {
            stopForegroundService();
        }
        return START_STICKY;
    }

    /**
     * Called when user clicks the "play/pause" button on the on-going system Notification.
     */
    public static class NotificationPlayButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            Toast.makeText(context, "Play Clicked " + action, Toast.LENGTH_SHORT).show();
            rhymesService.toggleTimerHandler();
        }
    }

    /**
     * Called when user clicks the "skip" button on the on-going system Notification.
     */
    public static class NotificationSkipButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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
            Toast.makeText(context, "Close Clicked", Toast.LENGTH_SHORT).show();
            //TODO: hide notification
        }
    }

// QUERIES

    private String runRhymesQuery(String word) {
        if (word == null || word.length() == 0) {
            Log.v(LOG_TAG, "runRhymesQuery: word == null or == \"\" ");
            return "";
        }
        String rhymes = "";
        rhymes = dataBaseHelper.getRhymes(word);

        //  rhymes.replaceAll("\\\\n",System.getProperty("line.separator"));
        //rhymes=rhymes.replaceAll("\\n","\n");
        return rhymes;//+\\\n sdfsdfsdf";
    }

    /**
     * looks up every element in the database by creating: AsyncRhymesQueryTasks
     * @param words
     */
    public void asyncRhymesQuery(ArrayList<String> words) {
        for (int i = 0; i < words.size(); i++) {
            //String match = words.get(i); rhymes = runRhymesQuery(match);
            new AsyncRhymesQuery().execute(new AsynchRhymesQueryParamWrapper(i + 1, words.get(i)));
            //PARALLEL THREADS:
            //new AsyncRhymesQuery().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new AsynchRhymesQueryParamWrapper(i+1,words.get(i)));
        }
    }

    /**
     * Task used to run the rhyme-queries in background
     * <p>
     * fills rhymeResult-array. when finishhed sets display
     */
    private class AsyncRhymesQuery extends AsyncTask<AsynchRhymesQueryParamWrapper, Void, AsynchRhymesQueryParamWrapper> {
        @Override
        protected AsynchRhymesQueryParamWrapper doInBackground(AsynchRhymesQueryParamWrapper... query) {
            Log.d(LOG_TAG, "AsyncRhymesQuery doInBackground(): just run rhymes query with Nr.: " + query[0].nr + " and word " + query[0].word);
            query[0].rhymes = runRhymesQuery(query[0].word);
            return query[0];
        }

        @Override
        protected void onPostExecute(AsynchRhymesQueryParamWrapper result) {
            //rhymeResults.add(result.rhymes);
            if (result.nr == 1) {
                broadcastCommandToBaseActivity("coloredOutputTextView", result.rhymes);
            }
            //broadcastCommandToBaseActivity("addToRhymeResultVector", result.rhymes);
            rhymeResults.add(result.rhymes);
            Log.d(LOG_TAG, "AsyncRhymesQuery onPostExecute():  just added results of query " + result.nr + "( " + result.word + " ) to rhymeResults-Arraylist");
        }
    }

    private class AsyncRandomRhymesQuery extends AsyncTask<Integer, Void, rhymesapp.WordRhymesPair> {
        @Override
        protected rhymesapp.WordRhymesPair doInBackground(Integer... query) {
            // Log.d(LOG_TAG, "AsyncRhymesQuery doInBackground(): just run rhymes query with Nr.: "+query[0].nr + " and word "+query[0].word  );
            return dataBaseHelper.getRandWordRhymesPair();

        }
        /*
        @Override
        protected void onProgressUpdate(Integer... integers){

        }
        */

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
        /*
        @Override
        protected WordRhymesPair onPostExecute(WordRhymesPair result) {
            return result;
            //Log.d(LOG_TAG, "AsyncRhymesQuery onPostExecute():  just added results of query "+ result.nr + "( "+result.word +" ) to rhymeResults-Arraylist");
        }
        */
    }

    public boolean isDbReadyLoaded() {
        return dataBaseHelper.isDbReadyLoaded();
    }

    private void initDataProvider() {
        //myDbHelper = getInstance(this);
        /* TODO: Uncomment*/
        boolean cont = false;
        try {
           cont= dataBaseHelper.setUpInternalDataBase();
        } catch (IOException ioe) {
            exceptionsToErrormessages(ioe);
        }
        if(cont)dataBaseHelper.openDataBase();


        //myDbHelper.getTableNames();
    }

    protected void exceptionsToErrormessages(Exception ioe){
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


// AUTO RANDOM FUNCTION:

    private static Handler timerHandler = new Handler();
    public static int autoRandomSpeedinMS = 4000;
    public static boolean enableAutoRandom = false;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            findRandomWordPair();
            timerHandler.postDelayed(this, autoRandomSpeedinMS);
        }
    };

    public void findRandomWordPair() {
        //Toast.makeText(context, "findRandomWordPair()", Toast.LENGTH_SHORT).show();
        new AsyncRandomRhymesQuery().execute(0);
    }

    /**
     * Starts and Stops the auto-random timer loop
     */
    public void toggleTimerHandler() {
        enableAutoRandom = !enableAutoRandom;
        if (enableAutoRandom) {
            enableAutoRandom = true;
            timerHandler.postDelayed(timerRunnable, 4000);
        } else {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }


//   SERVICE / ACTIVITY COMMUNICATION

    public static final String BROADCAST_ACTION = "rhymesapp";
    PendingIntent pendingIntent;
    LocalBroadcastManager broadcaster;


    public void sendResult(String message) {
        Intent intent = new Intent(COPA_RESULT);
        if (message != null)
            intent.putExtra(COPA_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    //TODO RENAME ME:
    static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
    static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";

    public void broadcastCommandToBaseActivity(String type, String text) {
        //Intent textViewToGuiIntent = new Intent(context, RhymesBaseActivity.ServiceTextToGuiHandler.class);
        //PendingIntent textViewToGuiPendingIntent;

        Intent textViewToGuiIntent = new Intent(COPA_RESULT);
        textViewToGuiIntent.putExtra("TYPE", type);
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

    private void showRandomWordRhymesPair(WordRhymesPair wordRhymesPair) {
        if (wordRhymesPair == null) return;
        broadcastCommandToBaseActivity("inputTextView", wordRhymesPair.getWord());
        broadcastCommandToBaseActivity("coloredOutputTextView", wordRhymesPair.getRhymes());
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
        return mBinder;
    }



// TEXT TO SPEECH:

    public boolean enableTextToSpeech;

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

        //todo: just a test-mokup:s
        if (speechRecognitionIsRunning) onResults2(null);
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
        broadcastCommandToBaseActivity("clickableWordsToInputTextView", text);

        //emptyRhymeResultsArray();
        broadcastCommandToBaseActivity("emptyRhymeResultsVector", "");

        asyncRhymesQuery(voiceRecogSpeechMatches);
        //System.getProperty("line.separator"));  stringVar.replaceAll("\\\\n", "\\\n"); make sure your \n is in "\n" for it to work.
        //prepareAndSendTextView(rhymes);
    }

    //TODO: just  a test-mokup for voice recognition:
    public void onResults2(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> voiceRecogSpeechMatches = new ArrayList<>();
        voiceRecogSpeechMatches.add("Ämter");
        voiceRecogSpeechMatches.add("Ägäis");
        voiceRecogSpeechMatches.add("Äderchens");
        voiceRecogSpeechMatches = guiUtils.prepareSpeechMatches(voiceRecogSpeechMatches);
        String text = guiUtils.concatStringListItems(voiceRecogSpeechMatches, ", ");

        //guiUtils.setClickableWordsInTextView(inputTextView, str, guiUtils.getDelimiterIndexes(str, ", "));        //inputTextView.setText(text);
        broadcastCommandToBaseActivity("clickableWordsToInputTextView", text);

        //emptyRhymeResultsArray();
        broadcastCommandToBaseActivity("emptyRhymeResultsVector", "");

        asyncRhymesQuery(voiceRecogSpeechMatches);
        //System.getProperty("line.separator"));  stringVar.replaceAll("\\\\n", "\\\n"); make sure your \n is in "\n" for it to work.
        //prepareAndSendTextView(rhymes);
    }


}



