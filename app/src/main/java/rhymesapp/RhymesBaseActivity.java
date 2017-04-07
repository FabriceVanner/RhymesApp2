package rhymesapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
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

import static rhymesapp.Constatics.*;


public class RhymesBaseActivity extends Activity implements View.OnTouchListener , AlertDialogCallback<GuiUtils.DownloadOrCopyDialog>{ /*implements View.OnKeyListener */

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
    private Button randomQueryButton;
    private Button keysButton;
    private ProgressBar recvolumeProgrBar;
    private SeekBar textFieldsSizeBar;
    private SeekBar autoRandomSpeedBar;

    private ToggleButton associationsToggle;
    private ToggleButton autoRandomToggle;
    private ToggleButton textToSpeechToggle;
    private ToggleButton hmToggle;
    private ToggleButton wakeLockToggle;
    private ToggleButton serviceToggle;

// CLASS-OBJECTS
    private HelperActivity helper = null;
    private GuiUtils guiUtils;


    //gui settings
    private boolean enableSpeechRecognition;
    private boolean enableAutoRandom;
    //public int autoRandomSpeedinMS;

    // options
    private boolean enableHashMapPrefetch;
    private boolean enableService;
    //private boolean enableSpeechRecognition = true;

    //saving settings:
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;


    //ACITVITY WAKE LOCK
    PowerManager pm;
    PowerManager.WakeLock wl;
    protected boolean enableWakeLock = false;

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
        state.putBoolean("enableWakeLock", enableWakeLock);
        state.putBoolean("enableHashMapPrefetch", enableHashMapPrefetch);
        state.putCharSequence("outputTextViewText", outputTextView.getText());
        state.putCharSequence("inputTextViewText", inputTextView.getText());
     //   state.putSerializable("rhymeResultsArray", rhymeResults);

        // constatics.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        // no need to check for null of savedInstanceState here
    //    rhymeResults = (Vector<String>) savedInstanceState.getSerializable("rhymeResultsArray");
        /*
        if(speechRecognitionSwitch!=null) {
            speechRecognitionSwitch.setChecked(savedInstanceState.getBoolean("enableSpeechRecognition", Constatics.enableSpeechRecognitionDefault));
        }
        */

        enableHashMapPrefetch = (savedInstanceState.getBoolean("enableHashMapPrefetch", enableHashMapPrefetchDefault));
        if (hmToggle != null) {
            hmToggle.setChecked(enableHashMapPrefetch);
        }


        outputTextView.setText(savedInstanceState.getCharSequence("outputTextViewText"));
        inputTextView.setText(savedInstanceState.getCharSequence("inputTextViewText"));
        enableAutoRandom = savedInstanceState.getBoolean("enableAutoRandom", false);
        enableWakeLock = savedInstanceState.getBoolean("enableWakeLock", enableWakeLockDefault);
        //  enableTextToSpeech = (savedInstanceState.getBoolean("enableTextToSpeech", enableHashMapPrefetchDefault));


        // Log.v(TAG, "Inside of onRestoreInstanceState");
    }

    /**
     * store settings in file
     */
    protected void savePersistentSettings() {
        //editor.putBoolean("enableTextToSpeech", enableTextToSpeech);
        editor.putInt("autoRandomSpeedinMS", rhymesService.autoRandomSpeedinMS);
        editor.putBoolean("enableAutoRandom", enableAutoRandom);
        editor.putBoolean("loadHashMapPrefetch", enableHashMapPrefetch);
        editor.commit();

    }

    /**
     * restore settings from file
     */
    protected void loadPersistentSettings() {
        //SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        rhymesService.autoRandomSpeedinMS = (sharedPreferences.getInt("autoRandomSpeedinMS", autoRandomSpeedinMSDefault));
        enableAutoRandom = sharedPreferences.getBoolean("enableAutoRandom", false);
        //enableTextToSpeech = sharedPreferences.getBoolean("enableTextToSpeech", enableTextToSpeechDefault);
        enableHashMapPrefetch = sharedPreferences.getBoolean("loadHashMapPrefetch", enableHashMapPrefetchDefault);


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


        if ((savedInstanceState == null)) {
            enableSpeechRecognition = enableSpeechRecognitionDefault;
            enableHashMapPrefetch = enableHashMapPrefetchDefault;
            rhymesService.autoRandomSpeedinMS = autoRandomSpeedinMSDefault;
            enableWakeLock = enableWakeLockDefault;
        }
        //TODO http://blog.cindypotvin.com/saving-preferences-in-your-android-application/
        /** save settings in file*/
        /*
        sharedPreferences = this.getPreferences(MODE_PRIVATE);
        editor = sharedPreferences.edit();
        loadPersistentSettings();
        */

        /** continue app, while screen off*/
        //   pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //   wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

     //   rhymeResults = new Vector<>();

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        //For Service Broadcast
        // context.registerReceiver(broadcastReceiver, new IntentFilter( RhymesService.BROADCAST_ACTION ) );
        setUpPendingIntentForBroadcastToService();

        //gui elements assign
        /** */
        recvolumeProgrBar = (ProgressBar) findViewById(R.id.progressBar);
        voiceRecogButton = (Button) findViewById(R.id.voiceRecogButton);


        randomQueryButton = (Button) findViewById(R.id.randomQueryButton);
        autoRandomSpeedBar = (SeekBar) findViewById(R.id.autoRandomSpeedBar);
        autoRandomSpeedBar.setMax(15000);
        autoRandomSpeedBar.setBottom(3000);
        autoRandomSpeedBar.setProgress((int) autoRandomSpeedinMSDefault);

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


        // listeners etc.
        randomQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rhymesService.findRandomWordPair();
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
                    rhymesService.asyncRhymesQuery(inputWords);
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

                //// Local Service Binding Communication
                if (rhymesServiceIsBound) {
                    // Call a method from the LocalService.                // However, if this call were something that might hang, then this request should                // occur in a separate thread to avoid slowing down the activity performance.
                    rhymesService.toggleTimerHandler();
                }
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

        serviceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // SERVICE
                            /*
                    // use this to start and trigger a service
                    Intent intent= new Intent(this, RhymesService.class);
            // potentially add data to the intent
                    //i.putExtra("KEY1", "Value to be used by the service");

                    bindService(intent, rhymesServiceBindConnection,Context.BIND_AUTO_CREATE);
                    */
                //context.startService(i);
                // #########################  Use Service as with intent
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

        // Communication from service to activiy via Loacalbroadcast:
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent);
            }
        };
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume()");
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        // Communication from service to activiy via Loacalbroadcast:
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);


        // Local Service Binding Communication
        if (rhymesServiceIsBound) {
            unbindService(rhymesServiceBindConnection);
            rhymesServiceIsBound = false;
        }

        Log.d(LOG_TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Local Service Binding Communication
        Intent intent = new Intent(this, RhymesService.class);
        bindService(intent, rhymesServiceBindConnection, Context.BIND_AUTO_CREATE);


        // Communication from service to activiy via Loacalbroadcast:
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                new IntentFilter(RhymesService.COPA_RESULT)
        );

    }

    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "onPause()");

        //SERVICE:
        unbindService(rhymesServiceBindConnection);
        rhymesServiceIsBound = false;
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

    public void setOutputTextView(TextView outputTextView) {
        this.outputTextView = outputTextView;
    }

    public EditText getInputTextView() {
        return inputTextView;
    }

    public void setInputTextView(EditText inputTextView) {
        this.inputTextView = inputTextView;
    }



// ACTIVITY / SERVICE COMMUNICATION

    enum serviceToGuiBroadcast{};

    /**
    broadcasted intent from service gets split to commands
     */
    private void updateUI(Intent intent) {
        String type = intent.getStringExtra("TYPE");
        String text = intent.getStringExtra("TEXT");
        //Toast.makeText(this,"updateUI: "+ type+" "+text,Toast.LENGTH_SHORT).show();
        if (type == "coloredOutputTextView") {
            prepareAndSendColoredTextView(outputTextView, text);
        } else if (type == "inputTextView") {
            prepareAndSendTextView(inputTextView, text);
        } else if (type == "addToRhymeResultVector") {
         //   rhymeResults.add(text);
        } else if (type == "clickableWordsToInputTextView") {
            guiUtils.setClickableWordsInTextView(inputTextView, text, guiUtils.getDelimiterIndexes(text, ", "));
            inputTextView.setText(text);
        } else if (type == "emptyRhymeResultsVector") {
     //       rhymeResults.clear();
        }else if(type =="showDownloadOrCopyDialog"){
            //guiUtils.showDownloadOrCopyDialog(context, this);
            GuiUtils.showDownloadOrCopyDialog(context, this);
        }else if(type=="updateDownCopyProgress"){
            if (text =="0")progressDialog= GuiUtils.setUpProgressDialog("Copying / Downloading",this);
            progressDialog.setProgress(Integer.valueOf(text));
        }
        //if (result.nr == 1) prepareAndSendColoredTextView(outputTextView, result.rhymes);
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
            rhymesServiceIsBound = false;
        }
    };

    //http://stackoverflow.com/questions/4195609/passing-arguments-to-asynctask-and-returning-results



    @Override
    public void alertDialogCallback(GuiUtils.DownloadOrCopyDialog ret) {
        rhymesService.dataBaseHelper.setUpInternalDataBasept2(ret);
    }


// PINCH ZOOM:

    final static float STEP = 200;
    float pinchMRatio = 1.0f;
    int pinchMBaseDist;
    float pinchMBaseRatio;
    float fontsize = 13;

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            int action = event.getAction();
            int pureaction = action & MotionEvent.ACTION_MASK;
            if (pureaction == MotionEvent.ACTION_POINTER_DOWN) {
                pinchMBaseDist = getDistance(event);
                pinchMBaseRatio = pinchMRatio;
            } else {
                float delta = (getDistance(event) - pinchMBaseDist) / STEP;
                float multi = (float) Math.pow(2, delta);
                pinchMRatio = Math.min(1024.0f, Math.max(0.1f, pinchMBaseRatio * multi));
                float Textsize = pinchMRatio + 13;
                if (Textsize > 22) {
                    Textsize = 22;
                } else if (Textsize < 13) {
                    Textsize = 13;
                }

                outputTextView.setTextSize(Textsize);
                Log.d(LOG_TAG, "mRation = " + pinchMRatio + "\tTextSize = " + Textsize);

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
        return false;
    }

    /**
     * AlertDialogCallback is necessary for callback dialog if database is missing
     * since the Dialog is async and without callback code would continue without waiting for an answer
     */


}