package rhymesapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

/**
 * Created by Fabrice Vanner on 22.01.2017.
 * http://www.gregbugaj.com/?p=47
 * find out how fast internet is.
 * Intended to find out which voice recognition (local or web) to use
 */
public class InternetSpeed {


    private static InternetSpeed internetSpeedSingleton;

    public static synchronized InternetSpeed getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (internetSpeedSingleton == null) {
            internetSpeedSingleton = new InternetSpeed(context.getApplicationContext());
        }
        return internetSpeedSingleton;
    }



    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    Context context;

    public InternetSpeed(Context context){
        this.context = context;
    }

    public void getInternetDownloadSpeed(){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            Integer linkSpeed = wifiInfo.getLinkSpeed(); //measured using WifiInfo.LINK_SPEED_UNITS
        }
    }

    private final Handler mHandler=new Handler(){
        @Override
        public void handleMessage(final Message msg) {
            switch(msg.what){
                case MSG_UPDATE_STATUS:
                    final SpeedInfo info1=(SpeedInfo) msg.obj;
                 //   mTxtSpeed.setText(String.format(getResources().getString(R.string.update_speed), mDecimalFormater.format(info1.kilobits)));
                    // Title progress is in range 0..10000
                //    setProgress(100 * msg.arg1);
                   // mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg2, EXPECTED_SIZE_IN_BYTES));
                    break;
                case MSG_UPDATE_CONNECTION_TIME:
                    //mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
                    break;
                case MSG_COMPLETE_STATUS:
                    final  SpeedInfo info2=(SpeedInfo) msg.obj;
                    //mTxtSpeed.setText(String.format(getResources().getString(R.string.update_downloaded_complete), msg.arg1, info2.kilobits));

                    //mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg1, EXPECTED_SIZE_IN_BYTES));

                    if(networkType(info2.kilobits)==1){
                      //  mTxtNetwork.setText(R.string.network_3g);
                    }else{
                       // mTxtNetwork.setText(R.string.network_edge);
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /**
     * Our Slave worker that does actually all the work
     */
    private final Runnable mWorker=new Runnable(){

        @Override
        public void run() {
            InputStream stream=null;
            try {
                int bytesIn=0;
                String downloadFileUrl="http://www.gregbugaj.com/wp-content/uploads/2009/03/dummy.txt";
                long startCon=System.currentTimeMillis();
                URL url=new URL(downloadFileUrl);
                URLConnection con=url.openConnection();
                con.setUseCaches(false);
                long connectionLatency=System.currentTimeMillis()- startCon;
                stream=con.getInputStream();

                Message msgUpdateConnection=Message.obtain(mHandler, MSG_UPDATE_CONNECTION_TIME);
                msgUpdateConnection.arg1=(int) connectionLatency;
                mHandler.sendMessage(msgUpdateConnection);

                long start=System.currentTimeMillis();
                int currentByte=0;
                long updateStart=System.currentTimeMillis();
                long updateDelta=0;
                int  bytesInThreshold=0;

                while((currentByte=stream.read())!=-1){
                    bytesIn++;
                    bytesInThreshold++;
                    if(updateDelta>=UPDATE_THRESHOLD){
                        int progress=(int)((bytesIn/(double)EXPECTED_SIZE_IN_BYTES)*100);
                        Message msg=Message.obtain(mHandler, MSG_UPDATE_STATUS, calculate(updateDelta, bytesInThreshold));
                        msg.arg1=progress;
                        msg.arg2=bytesIn;
                        mHandler.sendMessage(msg);
                        //Reset
                        updateStart=System.currentTimeMillis();
                        bytesInThreshold=0;
                    }
                    updateDelta = System.currentTimeMillis()- updateStart;
                }

                long downloadTime=(System.currentTimeMillis()-start);
                //Prevent AritchmeticException
                if(downloadTime==0){
                    downloadTime=1;
                }

                Message msg=Message.obtain(mHandler, MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
                msg.arg1=bytesIn;
                mHandler.sendMessage(msg);
            }
            catch (MalformedURLException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }finally{
                try {
                    if(stream!=null){
                        stream.close();
                    }
                } catch (IOException e) {
                    //Suppressed
                }
            }

        }
    };

    /**
     * Get Network type from download rate
     * @return 0 for Edge and 1 for 3G
     */
    private int networkType(final double kbps){
        int type=1;//3G
        //Check if its EDGE
        if(kbps<EDGE_THRESHOLD){
            type=0;
        }
        return type;
    }

    /**
     *
     * 1 byte = 0.0078125 kilobits
     * 1 kilobits = 0.0009765625 megabit
     *
     * @param downloadTime in miliseconds
     * @param bytesIn number of bytes downloaded
     * @return SpeedInfo containing current speed
     */
    private SpeedInfo calculate(final long downloadTime, final long bytesIn){
        SpeedInfo info=new SpeedInfo();
        //from mil to sec
        long bytespersecond   =(bytesIn / downloadTime) * 1000;
        double kilobits=bytespersecond * BYTE_TO_KILOBIT;
        double megabits=kilobits  * KILOBIT_TO_MEGABIT;
        info.downspeed=bytespersecond;
        info.kilobits=kilobits;
        info.megabits=megabits;

        return info;
    }

    /**
     * Transfer Object
     * @author devil
     *
     */
    private static class SpeedInfo{
        public double kilobits=0;
        public double megabits=0;
        public double downspeed=0;
    }


    //Private fields
    private static final String TAG = "RA";
    private static final int EXPECTED_SIZE_IN_BYTES = 1048576;//1MB 1024*1024

    private static final double EDGE_THRESHOLD = 176.0;
    private static final double BYTE_TO_KILOBIT = 0.0078125;
    private static final double KILOBIT_TO_MEGABIT = 0.0009765625;



    private final int MSG_UPDATE_STATUS=0;
    private final int MSG_UPDATE_CONNECTION_TIME=1;
    private final int MSG_COMPLETE_STATUS=2;

    private final static int UPDATE_THRESHOLD=300;


    private DecimalFormat mDecimalFormater;


}
