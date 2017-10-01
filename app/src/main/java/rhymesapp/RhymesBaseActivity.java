package rhymesapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
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

import java.util.ArrayList;

import static android.graphics.Color.parseColor;
import static rhymesapp.Constatics.ACTION.*;
import static rhymesapp.Constatics.*;
import static rhymesapp.RhymesBaseActivity.DownloadOrCopyDialog.CANCEL;
import static rhymesapp.RhymesBaseActivity.DownloadOrCopyDialog.DOWNLOAD;
import static rhymesapp.RhymesService.AutoRandomType.QUERYWORD;
import static rhymesapp.RhymesService.AutoRandomType.QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT;
import static rhymesapp.RhymesService.enableAutoRandom;


public class RhymesBaseActivity extends Activity implements AlertDialogCallback<RhymesBaseActivity.DownloadOrCopyDialog> { /*implements View.OnKeyListener */

    private String LOG_TAG = "RA";
    private Context context;
    /**
     * Communication from service to activity
     */
    private static RhymesBaseActivity rhymesBaseActivity;

    // GUI ELEMENTS:
    private TextView outputTextView;
    private EditText inputTextView;
    private Button voiceRecogButton;
    private Button randomRhymeQueryButton;
    private Button keysButton;
    private ProgressBar recvolumeProgrBar;
    private SeekBar textFieldsFontSizeBar;
    private int textFieldsFontSize;
    private SeekBar autoRandomSpeedBar;
   // private SeekBar linearAutoRandomWordNrBar;

    private ToggleButton associationsToggle;
    //private ToggleButton autoRandomToggle;
    private ImageButton play_Pause_AutoRandomQueryImageButton;
    private ToggleButton textToSpeechToggle;
    private ToggleButton hmToggle;
    private ToggleButton hardwareButtonsToggle;
    private ToggleButton serviceToggle;
    private ToggleButton autoRandomTypeToggle;

    // CLASS-OBJECTS
    private HelperActivity helper = null;
    private GuiUtils guiUtils;


    //gui settings
    private boolean enableSpeechRecognition;
    //private static boolean enableAutoRandom = false;
    //public int autoRandomSpeedinMS;

    // options
    private boolean enableHashMapPrefetch;
    private boolean enableService;
    //private boolean enableSpeechRecognition = true;

    //saving settings:
    private SharedPreferences sharedPreferences;


    /**
     * to store the results retourned by the async rhyme-queries (for the speak-recog. results)
     */
    //  public Vector<String> rhymeResults;


    private InputMethodManager im;

// SETTINGS AND OPTIONS STORING

    @Override
    protected void onSaveInstanceState(Bundle state) {
        Log.d(LOG_TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(state);
        //  state.putSerializable("starttime", startTime);
        //TODO: hier evtl rausnehmen, was in persistent in einer datei abgelegt wird

        state.putBoolean("enableAutoRandom", enableAutoRandom);
        state.putBoolean("enableSpeechRecognition", enableSpeechRecognition);
        //state.putBoolean("enableTextToSpeech", enableTextToSpeech);
        //    state.putBoolean("enableWakeLock", enableWakeLock);
        state.putBoolean("enableHashMapPrefetch", enableHashMapPrefetch);
        state.putCharSequence("outputTextViewText", outputTextView.getText());
        state.putCharSequence("inputTextViewText", inputTextView.getText());

        // TODO: ist das sinnvoll das zu speichern?:  state.putBoolean("rhymesServiceIsBound",rhymesServiceIsBound);
        //   state.putSerializable("rhymeResultsArray", rhymeResults);

        // constatics.onSaveInstanceState(state);
    }


    protected void restoreInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "restoreInstanceState()");
        /*
Because the onCreate() method is called whether the system is creating a new instance of your activity or recreating a previous one, you must check whether the state Bundle is null before you attempt to read it. If it is null, then the system is creating a new instance of the activity, instead of restoring a previous one that was destroyed.
 */
        if ((savedInstanceState == null)) {
            rhymesService.isFreshlyStarted = true;
            enableSpeechRecognition = ENABLESPEECHRECOGNITIONDEFAULT;
            enableHashMapPrefetch = ENABLEHASHMAPPREFETCHDEFAULT;
            rhymesService.autoRandomSpeedinMS = AUTORANDOMSPEEDINMSDEFAULT;
            textFieldsFontSize = TEXTFIELDSFONTSIZEDEFAULT;
            //   enableWakeLock = enableWakeLockDefault;
        } else {
            rhymesService.isFreshlyStarted = false;
            enableHashMapPrefetch = (savedInstanceState.getBoolean("enableHashMapPrefetch", ENABLEHASHMAPPREFETCHDEFAULT));
            outputTextView.setText(savedInstanceState.getCharSequence("outputTextViewText"));
            inputTextView.setText(savedInstanceState.getCharSequence("inputTextViewText"));
            enableAutoRandom = savedInstanceState.getBoolean("enableAutoRandom", false);
            //enableWakeLock = savedInstanceState.getBoolean("enableWakeLock", enableWakeLockDefault);
            // enableTextToSpeech = (savedInstanceState.getBoolean("enableTextToSpeech", enableHashMapPrefetchDefault));
            //    rhymesServiceIsBound=savedInstanceState.getBoolean("rhymesServiceIsBound",false);

            // wichtig beim rückholen der app aus dem background, damit der Button dem aktuallen zustand des service angepasst wird
            //TODO: wirft unregelmäßig fehler beim starten der App(wahrscheinlich bei hot Code replase): weil zu diesem Zeitpunkt der Button noch nicht initialisiert ist
            actualizePlayPauseImageButton();
        }
        if (hmToggle != null) {
            hmToggle.setChecked(enableHashMapPrefetch);
        }


        //TODO http://blog.cindypotvin.com/saving-preferences-in-your-android-application/


        /** save settings in file*/
        /*
        sharedPreferences = this.getPreferences(MODE_PRIVATE);
        editor = sharedPreferences.edit();
        loadPersistentSettings();
        */

        //    rhymeResults = (Vector<String>) savedInstanceState.getSerializable("rhymeResultsArray");
        /*
        if(speechRecognitionSwitch!=null) {
            speechRecognitionSwitch.setChecked(savedInstanceState.getBoolean("enableSpeechRecognition", Constatics.enableSpeechRecognitionDefault));
        }
        */


    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        // no need to check for null of savedInstanceState here
        restoreInstanceState(savedInstanceState);
        // Log.v(TAG, "Inside of onRestoreInstanceState");
    }

    /**
     * store settings in file
     */
    protected void savePersistentSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //editor.putBoolean("enableTextToSpeech", enableTextToSpeech);
        editor.putInt("autoRandomSpeedinMS", rhymesService.autoRandomSpeedinMS);
        editor.putBoolean("enableAutoRandom", rhymesService.enableAutoRandom);
        editor.putInt("textFieldsFontSize", textFieldsFontSize);
        // editor.putBoolean("loadHashMapPrefetch", enableHashMapPrefetch);
        editor.commit();

    }

    /**
     * restore settings from file
     */
    protected void loadPersistentSettings() {

        rhymesService.autoRandomSpeedinMS = (sharedPreferences.getInt("autoRandomSpeedinMS", AUTORANDOMSPEEDINMSDEFAULT));
        rhymesService.enableAutoRandom = sharedPreferences.getBoolean("enableAutoRandom", false);
        //TODO: noch kaputt:
        this.textFieldsFontSize = sharedPreferences.getInt("textFieldsFontSize", TEXTFIELDSFONTSIZEDEFAULT);
        //enableTextToSpeech = sharedPreferences.getBoolean("enableTextToSpeech", enableTextToSpeechDefault);
        //    enableHashMapPrefetch = sharedPreferences.getBoolean("loadHashMapPrefetch", enableHashMapPrefetchDefault);


    }

// LIFE-CYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate()");
        //http://stackoverflow.com/questions/4553605/difference-between-onstart-and-onresume
        super.onCreate(savedInstanceState);

        context = this.getApplicationContext();
        rhymesBaseActivity = this;
        helper = new HelperActivity(this);
        guiUtils = GuiUtils.getInstance(context);

        this.sharedPreferences = getPreferences(MODE_PRIVATE);
        if (savedInstanceState == null) {
            loadPersistentSettings();
        } else {
            restoreInstanceState(savedInstanceState);
        }


        //   rhymeResults = new Vector<>();

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        //For Service Broadcast
        // context.registerReceiver(broadcastReceiver, new IntentFilter( RhymesService.BROADCAST_ACTION ) );
        setUpPendingIntentForBroadcastToService();

        //gui elements assign
        /** an audio meter showing loudness of voice speech input*/
        recvolumeProgrBar = (ProgressBar) findViewById(R.id.progressBar);
        /** starts the voice recognition "recording"*/
        voiceRecogButton = (Button) findViewById(R.id.voiceRecogButton);

        /** performs a single random rhyme-QUERYTYPEDEFAULT*/
        randomRhymeQueryButton = (Button) findViewById(R.id.randomRhymeQueryButton);

        /** sets the frequence for automatic rhyme queries*/
        autoRandomSpeedBar = (SeekBar) findViewById(R.id.autoRandomSpeedBar);
        autoRandomSpeedBar.setMax(20000);
        autoRandomSpeedBar.setBottom(3000);
        autoRandomSpeedBar.setProgress((int) rhymesService.autoRandomSpeedinMS);

        /*
        linearAutoRandomWordNrBar = (SeekBar) findViewById(R.id.linearAutoRandomWordNrBar);
        linearAutoRandomWordNrBar.setMax(20);
        autoRandomSpeedBar.setBottom(3);
        autoRandomSpeedBar.setProgress(10);
*/
        outputTextView = (TextView) findViewById(R.id.outputTextView);
        outputTextView.setTextSize(textFieldsFontSize);
        inputTextView = (EditText) findViewById(R.id.inputText);
        inputTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        inputTextView.setTextSize(textFieldsFontSize);
        //inputTextView.setEnabled(true);

        /** adjust rhyme-queries to find associations*/
        associationsToggle = (ToggleButton) findViewById(R.id.associationsToggle);

        /** enables the automatic rhyme QUERYTYPEDEFAULT-function */
        //  autoRandomToggle = (ToggleButton) findViewById(R.id.autoRandomToggle);
        play_Pause_AutoRandomQueryImageButton = (ImageButton) findViewById(R.id.play_Pause_ImageButton);
        /**enables the text to speech synthetic voice output*/
        textToSpeechToggle = (ToggleButton) findViewById(R.id.voiceOutToggle);

        hardwareButtonsToggle = (ToggleButton) findViewById(R.id.hardwareButtonsToggle);
        // serviceToggle = (ToggleButton) findViewById(R.id.serviceToggle);
        autoRandomTypeToggle = (ToggleButton) findViewById(R.id.autoRandomTypeToggle);

        /** toggles the on-display keyboard*/
        keysButton = (Button) findViewById(R.id.keys);
        im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        /** sets the font-sizes of the text-displays*/
        textFieldsFontSizeBar = (SeekBar) findViewById(R.id.outputSizeBar);
        textFieldsFontSizeBar.setMax(80);
        textFieldsFontSizeBar.setBottom(16);
        textFieldsFontSizeBar.setProgress((int) TEXTFIELDSFONTSIZEDEFAULT);

        /** deprecated, hashmap queries are not necessary*/
        hmToggle = (ToggleButton) findViewById(R.id.hashMapToggle);
        hmToggle.setChecked(DataBaseHelper.isEnabledHashMapPrefetch());

        //outputTextView.requestFocus();
        recvolumeProgrBar.setVisibility(View.INVISIBLE);


        // listeners etc.
        randomRhymeQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rhymesService.queryType= RhymesService.QueryType.RHYME;
                rhymesService.randomQuery();
            }
        });
        autoRandomSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rhymesService.autoRandomSpeedinMS = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });
/*

        linearAutoRandomWordNrBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rhymesService.linearAutoRandomWordNr = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });
*/
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

                    if (!rhymesService.isDbReadyLoaded()) return false;

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

                    ArrayList<Integer> delimiterIndexes = guiUtils.getDelimiterIndexes(viewString, ", ");
                    guiUtils.setClickableWordsInTextView(inputTextView, viewString, delimiterIndexes);

                    rhymesService.rhymeResults.clear();
                    rhymesService.asyncWordQuery(inputWords);
                    return true;
                } else {
                    return false;
                }
            }
        });


        textFieldsFontSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //float inputTextSize = outputTextView.getTextSize();
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
/*
        hmToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //loadHashMapPrefetch = isChecked;
                constatics.setEnableHashMapPrefetch(isChecked);
                if (isChecked) {
                    dataBaseHelper.loadHashMapPrefetch();
                } else {
                    dataBaseHelper.setWordIndexHashMap(null);
                    Log.d(LOG_TAG, "hmSwitch: just set HashMap to null ");
                }
            }
        });
        */
        hardwareButtonsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //loadHashMapPrefetch = isChecked;
                if (isChecked) {
                    enableHardwareButtons = true;
                } else {
                    enableHardwareButtons = false;
                }
            }
        });
        //TODO: make it a button not a toggle
        associationsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //loadHashMapPrefetch = isChecked;
                if (isChecked) {
                    rhymesService.queryType= RhymesService.QueryType.ASSOCIATION;
                    rhymesService.randomQuery();
                } else {

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
                            rhymesService.toggleVoiceRecognition();
                            //TODO: -->fragmeint
                            recvolumeProgrBar.setVisibility(View.VISIBLE);
                            recvolumeProgrBar.setIndeterminate(true);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setBackgroundColor(Color.GREEN);
                        inputTextView.setVisibility(View.VISIBLE);
                        if (enableSpeechRecognition) {
                            rhymesService.toggleVoiceRecognition();
                            //TODO: -->fragmeint
                            recvolumeProgrBar.setIndeterminate(false);
                            recvolumeProgrBar.setVisibility(View.INVISIBLE);
                        }
                        v.setBackgroundColor(parseColor("#002a6f"));
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


        play_Pause_AutoRandomQueryImageButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "play_Pause_AutoRandomQueryImageButton: onClick():");

                rhymesService.toggleAutoRandom();
                actualizePlayPauseImageButton();

            }


        });





        autoRandomTypeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    rhymesService.autoRandomType= QUERYWORD_AND_THEN_WITHINQUERYWORDRESULT;
                }else{
                    rhymesService.autoRandomType= QUERYWORD;
                }

            }
        });

        textToSpeechToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    rhymesService.enableTextToSpeech = true;
                } else {
                    rhymesService.enableTextToSpeech = false;
                }
            }


        });
/*
S        serviceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // SERVICE

                // #########################  Use Service as with intent
                Intent serviceIntent = new Intent(RhymesBaseActivity.this, RhymesService.class);
                if (isChecked) {
                    if (!RhymesService.IS_FOREGROUND_SERVICE_RUNNING) {
                        serviceIntent.setAction(Constatics.ACTION.STARTFOREGROUND_ACTION);
                        RhymesService.IS_FOREGROUND_SERVICE_RUNNING = true;
                        startService(serviceIntent);

                    }
                } else {
                    if (RhymesService.IS_FOREGROUND_SERVICE_RUNNING) {
                        serviceIntent.setAction(Constatics.ACTION.STOPFOREGROUND_ACTION);
                        RhymesService.IS_FOREGROUND_SERVICE_RUNNING = false;
                        startService(serviceIntent);
                    }
                }
            }

        });
*/
        // Communication from service to activiy via Loacalbroadcast:
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                updateUI(intent);
            }
        };




        /* TODO: Brauche ich das noch für einen bound service?
        Intent serviceIntent = new Intent(RhymesBaseActivity.this, RhymesService.class);
            //if (!RhymesService.IS_FOREGROUND_SERVICE_RUNNING) {
                serviceIntent.setAction(Constatics.ACTION.STARTFOREGROUND_ACTION);
                RhymesService.IS_FOREGROUND_SERVICE_RUNNING = true;
                startService(serviceIntent);

         //   }
*/
    }

    public void  actualizePlayPauseImageButton() {


        if (rhymesService.enableAutoRandom) {
            rhymesBaseActivity.play_Pause_AutoRandomQueryImageButton.setBackgroundColor(parseColor("#000000"));
        } else {
            rhymesBaseActivity.play_Pause_AutoRandomQueryImageButton.setBackgroundColor(parseColor("#d30e63"));
        }
    }


    //todo: autohide:  moveTaskToBack(true);
    @Override
    public void onRestart() {
        Log.d(LOG_TAG, "onRestart()");
        // Local Service Binding Communication

        super.onRestart();
        //  onRestoreInstanceState()
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume()");
        super.onResume();
        actualizePlayPauseImageButton();

    }

    /**
     *
     */
    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy():");
        super.onDestroy();
        //TODO: nötig und richtig?
        //stop service as activity beeing destroyed
        /*
        isFinishing – returns true if the activity is finishing. It’s not called when the activity is destroyed because of an orientation change. It is called when we dismiss the activity. We want to stop the Service when the activity is destroyed and won’t be brought back to life
                //
         */
        if (isFinishing()) {
            Log.d(LOG_TAG, "onDestroy(): isFinishing()==true");
            /** TODO: alle beide methoden nötig? reihenfolge?*/

            savePersistentSettings();
            Log.d(LOG_TAG, "onDestroy(): stopService()");

            // Local Service Binding Communication
            if (rhymesServiceIsBound) {
                Log.d(LOG_TAG, "onStop(): rhymesServiceIsBound==true");
                Log.d(LOG_TAG, "onStop(): rhymesService.stopTimerHandler();");
                rhymesService.stopTimerHandler();


                Log.d(LOG_TAG, "onStop(): rhymesServiceIsBound==true : unbindService()");
                unbindService(rhymesServiceBindConnection);
                Log.d(LOG_TAG, "onStop(): rhymesServiceIsBound=false");
                rhymesServiceIsBound = false;
            }
            rhymesService.stopForegroundService();
            //    Intent intentStopService = new Intent(this, RhymesService.class);
            //   stopService(intentStopService); // dasselbe wie stopSelf() siehe RhymesService.stopForegroundService

        }
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop()");
        // Communication from service to activiy via Loacalbroadcast:

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        /* wenn ich ihn hier unbinde lässt er sich nachdem die act einmal im hintergrund war nicht mehr stoppen

        */
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart()");
        //You should usually not bind in onResume() and unbind in onPause()
        // Local Service Binding Communication
        Intent intent = new Intent(this, RhymesService.class);
        //TODO: rhymesServiceIsBound=(vom rückgabewert von bindservice setzen lassen?
        if (!rhymesServiceIsBound) {
            bindService(intent, rhymesServiceBindConnection, Context.BIND_AUTO_CREATE);

        }
        // Communication from service to activiy via Loacalbroadcast:
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(COLOREDOUTPUTTEXTVIEW_ACTION);
        intentFilter.addAction(INPUTTEXTVIEW_ACTION);
        intentFilter.addAction(TOGGLEPLAY_ACTION);
        intentFilter.addAction(ADDTORHYMERESULTVECTOR_ACTION);
        intentFilter.addAction(CLICKABLEWORDSTOINPUTTEXTVIEw_ACTION);
        intentFilter.addAction(EMPTYRHYMERESULTSVECTOR_ACTION);
        intentFilter.addAction(SHOWDOWNLOADORCOPYDIALOG_ACTION);
        intentFilter.addAction(UPDATEDOWNCOPYPROGRESS_ACTION);
        intentFilter.addAction(SHOWDOWNLOADDIALOG_ACTION);
        intentFilter.addAction(OUTPUTTEXTVIEW_ACTION);
        intentFilter.addAction(CLOSEAPP_ACTION);
        intentFilter.addAction(UPDATEAUTORANDOMBUTTON_ACTION);
        intentFilter.addAction(RANDOMQUERY_ACTION);
        intentFilter.addAction(MAIN_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                intentFilter
        );
        super.onStart();
    }

    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "onPause()");
        super.onPause();

    }


// GUI

    /**
     * colorizes text and sends it to output view
     */
    public void prepareAndSendTextView(TextView view, String text) {
        view.setText(text);
    }

    public void prepareAndSendColoredTextView(TextView view, String text) {
        Spannable spannable = guiUtils.colorizeText(text, "\n");
        view.setText(spannable, TextView.BufferType.SPANNABLE);
        //view.setText(text);

    }

    public TextView getOutputTextView() {
        return outputTextView;
    }

// ACTIVITY / SERVICE COMMUNICATION

    enum serviceToGuiBroadcast {outputTextView, coloredOutputTextView, inputTextView, addToRhymeResultVector, clickableWordsToInputTextView, emptyRhymeResultsVector, showDownloadOrCopyDialog, updateDownCopyProgress, showDownloadDialog}

    ;

    /**
     * broadcasted intents from service get executed
     */
    private void updateUI(Intent intent) {
        String type = intent.getAction();
        //String type = intent.getStringExtra("TYPE");
        String text = intent.getStringExtra("TEXT");
        //Toast.makeText(this,"updateUI: "+ type+" "+text,Toast.LENGTH_SHORT).show();


        if (type != COLOREDOUTPUTTEXTVIEW_ACTION && type != INPUTTEXTVIEW_ACTION) {
            Log.d(LOG_TAG, "updateUI(): type = " + type);
        }


        if (type == COLOREDOUTPUTTEXTVIEW_ACTION) {
            outputTextView.scrollTo(0, 0);
            prepareAndSendColoredTextView(outputTextView, text);
        } else if (type == INPUTTEXTVIEW_ACTION) {
            prepareAndSendTextView(inputTextView, text);
        } else if (type == TOGGLEPLAY_ACTION) {
            Toast.makeText(this, "RhymesBaseActiviy: received toggle Play", Toast.LENGTH_SHORT);
        } else if (type == ADDTORHYMERESULTVECTOR_ACTION) {
            //   rhymeResults.add(text);
        } else if (type == CLICKABLEWORDSTOINPUTTEXTVIEw_ACTION) {
            guiUtils.setClickableWordsInTextView(inputTextView, text, guiUtils.getDelimiterIndexes(text, ", "));
            inputTextView.setText(text);
        } else if (type == EMPTYRHYMERESULTSVECTOR_ACTION) {
            //       rhymeResults.clear();
        } else if (type == SHOWDOWNLOADORCOPYDIALOG_ACTION) {
            //guiUtils.showDownloadOrCopyDialog(context, this);
            showDownloadOrCopyDialog(context, this);
        } else if (type == UPDATEDOWNCOPYPROGRESS_ACTION) {
            progressDialog.setProgress(Integer.valueOf(text));
        } else if (type == SHOWDOWNLOADDIALOG_ACTION) {
            showDownloadOrCopyDialog(context, this);
        } else if (type == OUTPUTTEXTVIEW_ACTION) {
            outputTextView.scrollTo(0, 0);
            prepareAndSendTextView(outputTextView, text);
            //} else if ( type == "toggleNotification_button_play_image"){
        } else if (type == UPDATEAUTORANDOMBUTTON_ACTION) {
            actualizePlayPauseImageButton();
            //       rhymesService.mNotificationManager.notify(101,rhymesService.mNotifyBuilder.build());
            //     findViewById(R.id.notification_button_play)).setImageResource();
        } else if (type == CLOSEAPP_ACTION) {
            /** TODO: does not work...*/
            finish();
            onDestroy();
        } else if (type == RANDOMQUERY_ACTION) {
            //TODO: ist etwas umständlich: Notification ruft vom Service über broadcast die activity an damit diese wieder die Methode im Service auslöst
            rhymesService.randomQuery();

        }


        //if (result.nr == 1) prepareAndSendColoredTextView(outputTextView, result.queryResult);
    }

    ProgressDialog progressDialog;

    /**
     * for broadcasting to service
     */
    PendingIntent pendingIntent;

    /**
     * COMMUNINICATION FROM ACTIVITY TO SERVICE VIA PENDING INTENT
     * TODO: vielleicht obsolet, wenn ich nur bound service benutze
     */
    protected void setUpPendingIntentForBroadcastToService() {
        Intent notificationIntent = new Intent(context, RhymesBaseActivity.class);
        notificationIntent.setAction(Constatics.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
    }

    // Communication from service to activiy via Localbroadcast:
    BroadcastReceiver broadcastReceiver;


    // Local Service Binding Communication
    RhymesService rhymesService;
    boolean rhymesServiceIsBound = false;
    /**
     * Local Service Binding Communication: Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection rhymesServiceBindConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RhymesService.LocalBinder binder = (RhymesService.LocalBinder) service;
            rhymesService = binder.getService();
            rhymesServiceIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //    rhymesService= null; TODO: bracuh ich das?
            rhymesServiceIsBound = false;
        }
    };

    //http://stackoverflow.com/questions/4195609/passing-arguments-to-asynctask-and-returning-results


// PINCH ZOOM:


    // DIALOGS:
    @Override
    public void alertDialogCallback(DownloadOrCopyDialog ret) {
        // ist nötig da der alert nebenläufig gestartet wird, also die anwendung nicht stoppt...
        //   ProgressDialog progressDialog;
        switch (ret) {
            case CANCEL:
                return;
            case DOWNLOAD:
                Toast.makeText(context, "Downloading... ", Toast.LENGTH_LONG).show();
                progressDialog = GuiUtils.setUpProgressDialog("Downloading DB", this);
                rhymesService.dataBaseHelper.downloadDb(dbURL, "", "test.png");
                return;
            case COPY:
                progressDialog = GuiUtils.setUpProgressDialog("Copying DB", this);
                //   rhymesService.dataBaseHelper.copyDBifNecessary(true,progressDialog);
                rhymesService.dataBaseHelper.openDataBase();
                return;
        }
    }


    /**
     * AlertDialogCallback is necessary for callback dialog if database is missing
     * since the Dialog is async and without callback code would continue without waiting for an answer
     */
    enum DownloadOrCopyDialog {
        DOWNLOAD, COPY, CANCEL, UNSET
    }

    public void showDownloadOrCopyDialog(Context context, final AlertDialogCallback<DownloadOrCopyDialog> callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(RhymesBaseActivity.this);
        builder.setMessage("Theres no DB present on internal and external Storage. \nWould you like to download from Web?");
        builder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.alertDialogCallback(DOWNLOAD);
            }
        });
   /*     builder.setNegativeButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.alertDialogCallback(COPY);
            }
        });*/
        builder.setNeutralButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.alertDialogCallback(CANCEL);
            }
        });
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        //TODO: Unable to create service rhymesapp.RhymesService: android.view.WindowManager$BadTokenException: Unable to add window android.view.ViewRootImpl$W@eb93714 -- permission denied for this window type at android.app.ActivityThread.handleCreateService(ActivityThread.java:2921)
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION);//TYPE_SYSTEM_ALERT);
        alert.setCanceledOnTouchOutside(false);
        alert.show();
        //dialog.dismiss();
    }


    // Key-eventzs

    boolean enableHardwareButtons = false;

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (enableHardwareButtons) {

            switch (keyCode) {
                case KeyEvent.KEYCODE_MENU:
                    Toast.makeText(this, "Menu key released", Toast.LENGTH_SHORT).show();
                    return true;
                case KeyEvent.KEYCODE_SEARCH:
                    Toast.makeText(this, "Search key released", Toast.LENGTH_SHORT).show();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (event.isTracking() && !event.isCanceled())
                        rhymesService.randomQuery();
                    Toast.makeText(this, "Volumen Up released", Toast.LENGTH_SHORT).show();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    rhymesService.randomQuery();
                    Toast.makeText(this, "Volumen Down released", Toast.LENGTH_SHORT).show();
                    return true;
            }
            return super.onKeyDown(keyCode, event);
        }
        return false;
    }

}