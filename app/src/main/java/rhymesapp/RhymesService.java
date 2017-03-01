package rhymesapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.rhymesapp.R;

public class RhymesService extends Service {

    //http://blog.nkdroidsolutions.com/android-foreground-service-example-tutorial/




    private final IBinder mBinder = new MyBinder();
    public static  boolean IS_SERVICE_RUNNING = false;

    private static BaseActToServiceStorage baseActToServiceStorage;
    private static final String LOG_TAG = "RhymesService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BaseActToServiceStorage.context = this.getApplicationContext();
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
            buttonPlayIntent.putExtra("action", "togglePause");

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

            if (action=="start") baseActToServiceStorage.startTimerHandler();
            else   baseActToServiceStorage.stopTimerHandler();
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





    public class MyBinder extends Binder {
        RhymesService getService() {
            return RhymesService.this;
        }
    }

    public RhymesService() {
        System.out.println();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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
}
