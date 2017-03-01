package rhymesapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.rhymesapp.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;

import static rhymesapp.Constatics.*;
import static rhymesapp.StringsAndStuff.ERR_NOT_OPEN_DB;


public class RhymesBaseActivity extends Activity implements RecognitionListener, View.OnTouchListener, TextToSpeech.OnInitListener { /*implements View.OnKeyListener */


    private TextView outputTextView;
    private EditText inputTextView;
    private Button voiceRecogButton;
    private Button randomQueryButton;
    private Button keysButton;
    private ProgressBar recvolumeProgrBar;
    private SeekBar textFieldsSizeBar;
    private SeekBar autoRandomSpeedBar;
    private SpeechRecognizer speech = null;

    private ToggleButton associationsToggle;
    private ToggleButton autoRandomToggle;
    private ToggleButton textToSpeechToggle;
    private ToggleButton hmToggle;
    private ToggleButton wakeLockToggle;
    private ToggleButton serviceToggle;
    //###############################################################################################
    private Intent recognizerIntent;
    private HelperActivity helper = null;
    private String LOG_TAG = "RA";
    private Handler timerHandler = new Handler();

    private Context context;
    /** for broadcasting to service*/
    PendingIntent pendingIntent;

    //private boolean enableSpeechRecognition = true;
// Pinch Zoom:
    final static float STEP = 200;

    float mRatio = 1.0f;
    int mBaseDist;
    float mBaseRatio;
    float fontsize = 13;
    //
    //private DataBaseHelper myDbHelper;
    private Constatics constatics;
    private boolean enableSpeechRecognition;
    private boolean enableTextToSpeech;
    private boolean enableAutoRandom;
    private boolean enableHashMapPrefetch;
    private boolean enableService;
    public int autoRandomSpeedinMS;

    //###############################################################################################
    TextToSpeech textToSpeechEngine;

    PowerManager pm;
    PowerManager.WakeLock wl;
    protected boolean enableWakeLock = false;

    /**
     * to store the results retourned by the async rhyme-queries (for the speak-recog. results)
     */
    public Vector<String> rhymeResults;


    public WordRhymesPair randomRhymesQuery;

    private InputMethodManager im;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    //###############################################################################################
    /*
    private RhymesService rhymesService;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            RhymesService.MyBinder b = (RhymesService.MyBinder) binder;
            rhymesService = b.getService();
            Toast.makeText(RhymesBaseActivity.this, "Connected", Toast.LENGTH_SHORT)
                    .show();
        }
        public void onServiceDisconnected(ComponentName className) {
            rhymesService = null;
        }
    };
*/
    public TextView getOutputTextView() {
        return outputTextView;
    }

    public void setOutputTextView(TextView outputTextView) {
        this.outputTextView = outputTextView;
    }

    public EditText getInputTextView() {
        return inputTextView;
    }

    public void setInputTextView(EditText inputTextView) {
        this.inputTextView = inputTextView;
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
            Toast.makeText(RhymesBaseActivity.this, ERR_NOT_OPEN_DB + mess, Toast.LENGTH_SHORT).show();
            return;
        }
        constatics.dataBaseHelper.openDataBase();


        //myDbHelper.getTableNames();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        Log.d(LOG_TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(state);
        //  state.putSerializable("starttime", startTime);
        //TODO: hier evtl rausnehmen, was in persistent in einer datei abgelegt wird

        state.putBoolean("enableAutoRandom", enableAutoRandom);
        state.putBoolean("enableSpeechRecognition", enableSpeechRecognition);
        state.putBoolean("enableTextToSpeech", enableTextToSpeech);
        state.putBoolean("enableWakeLock", enableWakeLock);
        state.putBoolean("enableHashMapPrefetch", enableHashMapPrefetch);
        state.putCharSequence("outputTextViewText", outputTextView.getText());
        state.putCharSequence("inputTextViewText", inputTextView.getText());
        state.putSerializable("rhymeResultsArray", rhymeResults);

        constatics.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        // no need to check for null of savedInstanceState here
        rhymeResults = (Vector<String>) savedInstanceState.getSerializable("rhymeResultsArray");
        /*
        if(speechRecognitionSwitch!=null) {
            speechRecognitionSwitch.setChecked(savedInstanceState.getBoolean("enableSpeechRecognition", Constatics.enableSpeechRecognitionDefault));
        }
        */

        enableHashMapPrefetch = (savedInstanceState.getBoolean("enableHashMapPrefetch", enableHashMapPrefetchDefault));
        if (hmToggle != null) {
            hmToggle.setChecked(enableHashMapPrefetch);
        }
        if (constatics != null) {
            constatics.onRestoreInstanceState(savedInstanceState);
        } else {
            constatics = Constatics.getInstance(this);
        }

        outputTextView.setText(savedInstanceState.getCharSequence("outputTextViewText"));
        inputTextView.setText(savedInstanceState.getCharSequence("inputTextViewText"));
        enableAutoRandom = savedInstanceState.getBoolean("enableAutoRandom", false);
        enableWakeLock = savedInstanceState.getBoolean("enableWakeLock", enableWakeLockDefault);
        enableTextToSpeech = (savedInstanceState.getBoolean("enableTextToSpeech", enableHashMapPrefetchDefault));


        // Log.v(TAG, "Inside of onRestoreInstanceState");
    }


    protected void setUpPendingIntentForBroadcastToService(){
        //##################### Service
        Intent notificationIntent = new Intent(context, RhymesBaseActivity.class);
        notificationIntent.setAction(Constatics.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
         pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate()");
        //http://stackoverflow.com/questions/4553605/difference-between-onstart-and-onresume
        super.onCreate(savedInstanceState);
        helper = new HelperActivity(this);
        if ((savedInstanceState == null)) {
            constatics = Constatics.getInstance(this);
            enableSpeechRecognition = enableSpeechRecognitionDefault;
            enableHashMapPrefetch = enableHashMapPrefetchDefault;
            autoRandomSpeedinMS = autoRandomSpeedinMSDefault;
            enableWakeLock = enableWakeLockDefault;
            initDataProvider();
        }
        //TODO http://blog.cindypotvin.com/saving-preferences-in-your-android-application/
        /** save settings in file*/
        /*
        sharedPreferences = this.getPreferences(MODE_PRIVATE);
        editor = sharedPreferences.edit();
        loadPersistentSettings();
        */

        /** continue app, while screen off*/
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

        rhymeResults = new Vector<>();

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


//######################For Service Broadcast#####################################################
//
        context = this.getApplicationContext();
        setUpPendingIntentForBroadcastToService();
//###############################################################################################
        /** */
        recvolumeProgrBar = (ProgressBar) findViewById(R.id.progressBar);
        voiceRecogButton = (Button) findViewById(R.id.voiceRecogButton);


        randomQueryButton = (Button) findViewById(R.id.randomQueryButton);
        autoRandomSpeedBar = (SeekBar) findViewById(R.id.autoRandomSpeedBar);
        autoRandomSpeedBar.setMax(15000);
        autoRandomSpeedBar.setBottom(3000);
        autoRandomSpeedBar.setProgress((int) autoRandomSpeedinMS);

        outputTextView = (TextView) findViewById(R.id.outputTextView);
        inputTextView = (EditText) findViewById(R.id.inputText);
        inputTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        //inputTextView.setEnabled(true);

        associationsToggle = (ToggleButton) findViewById(R.id.associationsToggle);
        autoRandomToggle = (ToggleButton) findViewById(R.id.autoRandomToggle);
        ;
        textToSpeechToggle = (ToggleButton) findViewById(R.id.voiceOutToggle);
        wakeLockToggle = (ToggleButton) findViewById(R.id.wakeLockToggle);
        serviceToggle = (ToggleButton) findViewById(R.id.serviceToggle);

        keysButton = (Button) findViewById(R.id.keys);
        im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        textFieldsSizeBar = (SeekBar) findViewById(R.id.outputSizeBar);
        textFieldsSizeBar.setMax(80);
        textFieldsSizeBar.setBottom(16);
        textFieldsSizeBar.setProgress((int) outputTextView.getTextSize());

        hmToggle = (ToggleButton) findViewById(R.id.hashMapToggle);
        hmToggle.setChecked(DataBaseHelper.isEnabledHashMapPrefetch());

        //outputTextView.requestFocus();
        recvolumeProgrBar.setVisibility(View.INVISIBLE);
        //###############################################################################################

        if (enableSpeechRecognition) {
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


        randomQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findRandomWordPair();
            }
        });
        autoRandomSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                autoRandomSpeedinMS = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });


        keysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputTextView.setCursorVisible(true);
                inputTextView.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

                im.showSoftInput(inputTextView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

/*
        inputTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                im.showSoftInput(inputTextView, InputMethodManager.SHOW_IMPLICIT);
            }
        });
*/

        inputTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || (actionId == EditorInfo.IME_ACTION_DONE) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))) {
                    if (!constatics.dataBaseHelper.isDbReadyLoaded()) return false;

                    // close keyboard:
                    //  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    //InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(inputTextView.getWindowToken(), 0);
                    //inputTextView.setCursorVisible(false);
                    //String str = inputTextView.getText().toString().split(" ")[0];
                    outputTextView.setText("");

                    String viewString = inputTextView.getText().toString();

                    ArrayList<String> inputWords = new ArrayList<>();
                    String[] inputWordsStrArr = inputTextView.getText().toString().split(", ");
                    for (String word : inputWordsStrArr) {
                        inputWords.add(word);
                    }

                    ArrayList<Integer> delimiterIndexes = Constatics.guiUtils.getDelimiterIndexes(viewString, ", ");
                    Constatics.guiUtils.setClickableWordsInTextView(inputTextView, viewString, delimiterIndexes);

                    emptyRhymeResultsArray();
                    asyncRhymesQuery(inputWords);

                    //new AsyncRhymesQueryTask().execute(new AsynchRhymesQueryParamWrapper(1,str));
                    //prepareAndSendViewText(runRhymesQuery(str));
                    //Toast.makeText(RhymesBaseActivity.this,"inputTextView.setOnKeyListener(): Running Rhymes query with :"+str, Toast.LENGTH_SHORT).show();
                    //inputTextView.setEnabled(false);
                    return true;
                } else {
                    return false;
                }
            }
        });


        textFieldsSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                outputTextView.setTextSize(progress);
                float inputTextSize = inputTextView.getTextSize();
                inputTextView.setTextSize(inputTextSize + (progress - inputTextSize));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        hmToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //loadHashMapPrefetch = isChecked;
                constatics.setEnableHashMapPrefetch(isChecked);
                if (isChecked) {
                    constatics.dataBaseHelper.loadHashMapPrefetch();
                } else {
                    constatics.dataBaseHelper.setWordIndexHashMap(null);
                    Log.d(LOG_TAG, "hmSwitch: just set HashMap to null ");
                }
            }
        });


        voiceRecogButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setBackgroundColor(Color.GREEN);
                        inputTextView.setVisibility(View.INVISIBLE);
                        if (enableSpeechRecognition) {
                            speech.startListening(recognizerIntent);
                            recvolumeProgrBar.setVisibility(View.VISIBLE);
                            recvolumeProgrBar.setIndeterminate(true);
                        }

                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setBackgroundColor(Color.GREEN);
                        inputTextView.setVisibility(View.VISIBLE);
                        if (enableSpeechRecognition) {
                            speech.stopListening();
                            recvolumeProgrBar.setIndeterminate(false);
                            recvolumeProgrBar.setVisibility(View.INVISIBLE);
                        }
                        v.setBackgroundColor(Color.parseColor("#002a6f"));
                        return true;
                }
                return false;
            }
        });


        outputTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchEvent(event);

            }
        });


        autoRandomToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAutoRandom = isChecked;
                Intent buttonPlayIntent = new Intent(context, RhymesService.NotificationPlayButtonHandler.class);
                PendingIntent buttonPlayPendingIntent;
                if (isChecked) {
                    //for service-broadcast
                    buttonPlayIntent.putExtra("action", "start");

                    if (enableWakeLock) {
                        wl.acquire();
                    }
                    timerHandler.postDelayed(timerRunnable, 4000);




                } else {
                    //for service-broadcast
                    buttonPlayIntent.putExtra("action", "stop");




                    timerHandler.removeCallbacks(timerRunnable);
                    if (enableWakeLock) {
                        wl.release();
                    }
                }
                buttonPlayPendingIntent = pendingIntent.getBroadcast(context, 0, buttonPlayIntent, 0);
                // for service broadcast
                try {
                    // Perform the operation associated with our pendingIntent
                    buttonPlayPendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }


        });

        textToSpeechToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableTextToSpeech = true;
                } else {
                    enableTextToSpeech = false;
                }
            }


        });

        serviceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent serviceIntent = new Intent(RhymesBaseActivity.this, RhymesService.class);

                if (isChecked) {
                    if (!RhymesService.IS_SERVICE_RUNNING) {
                        serviceIntent.setAction(Constatics.ACTION.STARTFOREGROUND_ACTION);
                        RhymesService.IS_SERVICE_RUNNING = true;
                        startService(serviceIntent);

                    }
                } else {
                    if (RhymesService.IS_SERVICE_RUNNING) {
                        serviceIntent.setAction(Constatics.ACTION.STOPFOREGROUND_ACTION);
                        RhymesService.IS_SERVICE_RUNNING = false;
                        startService(serviceIntent);
                    }
                }
            }

        });

    }

    private void findRandomWordPair() {
        new AsyncRandomRhymesQuery().execute(0);
    }

    private void showRandomWordRhymesPair(WordRhymesPair wordRhymesPair) {
        if (wordRhymesPair == null) return;
        prepareAndSendViewText(inputTextView, wordRhymesPair.getWord());
        prepareAndSendColoredViewText(outputTextView, wordRhymesPair.getRhymes());
    }


    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            findRandomWordPair();
            timerHandler.postDelayed(this, autoRandomSpeedinMS);
        }
    };

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            int action = event.getAction();
            int pureaction = action & MotionEvent.ACTION_MASK;
            if (pureaction == MotionEvent.ACTION_POINTER_DOWN) {
                mBaseDist = getDistance(event);
                mBaseRatio = mRatio;
            } else {
                float delta = (getDistance(event) - mBaseDist) / STEP;
                float multi = (float) Math.pow(2, delta);
                mRatio = Math.min(1024.0f, Math.max(0.1f, mBaseRatio * multi));
                float Textsize = mRatio + 13;
                if (Textsize > 22) {
                    Textsize = 22;
                } else if (Textsize < 13) {
                    Textsize = 13;
                }

                outputTextView.setTextSize(Textsize);
                Log.d(LOG_TAG, "mRation = " + mRatio + "\tTextSize = " + Textsize);

            }
        }
        return true;
    }

    int getDistance(MotionEvent event) {
        int dx = (int) (event.getX(0) - event.getX(1));
        int dy = (int) (event.getY(0) - event.getY(1));
        return (int) (Math.sqrt(dx * dx + dy * dy));
    }

    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume()");
        super.onResume();
/*
// SERVICE
        // use this to start and trigger a service
        Intent intent= new Intent(this, RhymesService.class);
// potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service");

        bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);
        //context.startService(i);
*/
    }


    /**
     * store settings in file
     */
    protected void savePersistentSettings() {
        editor.putBoolean("enableTextToSpeech", enableTextToSpeech);
        editor.putInt("autoRandomSpeedinMS", autoRandomSpeedinMS);
        editor.putBoolean("enableAutoRandom", enableAutoRandom);
        editor.putBoolean("enableTextToSpeech", enableTextToSpeech);
        editor.putBoolean("loadHashMapPrefetch", enableHashMapPrefetch);
        editor.commit();

    }

    /**
     * restore settings from file
     */
    protected void loadPersistentSettings() {
        //SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        autoRandomSpeedinMS = (sharedPreferences.getInt("autoRandomSpeedinMS", autoRandomSpeedinMSDefault));
        enableAutoRandom = sharedPreferences.getBoolean("enableAutoRandom", false);
        enableTextToSpeech = sharedPreferences.getBoolean("enableTextToSpeech", enableTextToSpeechDefault);
        enableHashMapPrefetch = sharedPreferences.getBoolean("loadHashMapPrefetch", enableHashMapPrefetchDefault);


    }


    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop()");
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "onPause()");
        //boolean isBound = false;

        //SERVICE:
//        unbindService(mConnection);



/*
        boolean speechServiceIsBound = getApplicationContext().bindService( new Intent(getApplicationContext(), SpeechRecognizer.class), Context.BIND_AUTO_CREATE );
        if (speechServiceIsBound)
            getApplicationContext().unbindService(serviceConnection);
*/
        super.onPause();
        if (speech != null) {
            //speak.destroy();
        }
        Constatics.dataBaseHelper.close();

    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        recvolumeProgrBar.setIndeterminate(false);
        recvolumeProgrBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        recvolumeProgrBar.setIndeterminate(true);
        //recvolumeProgrBar.setVisibility(View.INVISIBLE);
        //voiceRecogButton.setChecked(false);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        outputTextView.setText(errorMessage);
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
        voiceRecogSpeechMatches = Constatics.guiUtils.prepareSpeechMatches(voiceRecogSpeechMatches); //TODO:
        String str = Constatics.guiUtils.concatStringListItems(voiceRecogSpeechMatches, ", ");
        Constatics.guiUtils.setClickableWordsInTextView(inputTextView, str, Constatics.guiUtils.getDelimiterIndexes(str, ", "));
        //inputTextView.setText(text);

        emptyRhymeResultsArray();
        asyncRhymesQuery(voiceRecogSpeechMatches);
        //System.getProperty("line.separator"));  stringVar.replaceAll("\\\\n", "\\\n"); make sure your \n is in "\n" for it to work.
        //prepareAndSendViewText(rhymes);
    }
//iah\\nYue\\naua\\nAye-Aye\\na\\neh\\nhaha\\naha\\nAr\\nEth\\ndz\\nRäf\\nträf\\nMarseille\\nBouteille\\nNonpareilles\\nIschewsk\\nBischkek\\nquäk\\nerwäg\\nsäg\\nzersäg\\npräg\\nträg\\nschräg\\npfähl\\nmähl\\nvermähl\\nstähl\\nwähl\\n


    private void asyncRhymesQuery(ArrayList<String> matches) {
        for (int i = 0; i < matches.size(); i++) {
            //String match = matches.get(i); rhymes = runRhymesQuery(match);
            new AsyncRhymesQueryTask().execute(new AsynchRhymesQueryParamWrapper(i + 1, matches.get(i)));
            //PARALLEL THREADS:
            //new AsyncRhymesQueryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new AsynchRhymesQueryParamWrapper(i+1,matches.get(i)));
        }
    }


    private void emptyRhymeResultsArray() {
        rhymeResults.clear();
    }

    /**
     * colorizes text and sends it to output view
     */
    public void prepareAndSendViewText(TextView view, String text) {
        ///Spannable spannable = Constatics.guiUtils.colorizeText(text,"\n"); //TODO:
        //outputTextView.setText(spannable,TextView.BufferType.SPANNABLE);
        view.setText(text);

    }


    public void prepareAndSendColoredViewText(TextView view, String text) {
        Spannable spannable = Constatics.guiUtils.colorizeText(text, "\n"); //TODO:
        view.setText(spannable, TextView.BufferType.SPANNABLE);
        //view.setText(text);

    }


    /**
     * performs a check and starts the query
     */
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

    private void loadTextToSpeech() {
        textToSpeechEngine = new TextToSpeech(this, this);
    }

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

    private void setSpeech() {
        float pitch = 1.0f;
        float speed = 1.0f;
        textToSpeechEngine.setPitch((float) pitch);
        textToSpeechEngine.setSpeechRate((float) speed);
        //textToSpeechEngine.speak(editText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }


//http://stackoverflow.com/questions/4195609/passing-arguments-to-asynctask-and-returning-results


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
        /*
        @Override
        protected void onProgressUpdate(Integer... integers){

        }
        */

        @Override
        protected void onPostExecute(AsynchRhymesQueryParamWrapper result) {
            rhymeResults.add(result.rhymes);
            if (result.nr == 1) prepareAndSendColoredViewText(outputTextView, result.rhymes);
            Log.d(LOG_TAG, "AsyncRhymesQueryTask onPostExecute():  just added results of query " + result.nr + "( " + result.word + " ) to rhymeResults-Arraylist");
        }
    }


    private class AsyncRandomRhymesQuery extends AsyncTask<Integer, Void, rhymesapp.WordRhymesPair> {
        @Override
        protected rhymesapp.WordRhymesPair doInBackground(Integer... query) {
            // Log.d(LOG_TAG, "AsyncRhymesQueryTask doInBackground(): just run rhymes query with Nr.: "+query[0].nr + " and word "+query[0].word  );
            return Constatics.dataBaseHelper.getRandWordRhymesPair();

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
            //Log.d(LOG_TAG, "AsyncRhymesQueryTask onPostExecute():  just added results of query "+ result.nr + "( "+result.word +" ) to rhymeResults-Arraylist");
        }
        */
    }


    /**
     * used for peak-meter of audio recogn.
     *
     * @param rmsdB
     */
    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        recvolumeProgrBar.setProgress((int) rmsdB);
    }

    public static String getErrorText(int errorCode) {
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


}