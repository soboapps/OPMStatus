package com.soboapps.opm_status;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Avi on 1/24/2015.
 */
public class NotificationService extends Service {

    private static final String WAKE_TAG = "WAKE_TAG";
    private PowerManager.WakeLock mWakeLock;

    /**
     * Simply return null, since our Service will not be communicating with
     * any other components. It just does its work silently.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is where we initialize. We call this when onStart/onStartCommand is
     * called by the system. We won't do anything with the intent here, and you
     * probably won't, either.
     */
    private void handleIntent() {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG);
        mWakeLock.acquire();

        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) {
            stopSelf();
            return;
        }

        // do the actual work, in a separate thread
        new PollTask().execute();
    }

    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent();
        return START_NOT_STICKY;
    }

    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }

    private class PollTask extends AsyncTask<Void, Void, OPMStatus> {
        @Override
        protected OPMStatus doInBackground(Void... params) {
            JSONObject obj = SyncRestClient.connect();
            return new OPMStatus(obj);
        }

        /**
         * In here you should interpret whatever you fetched in doInBackground
         * and push any notifications you need to the status bar, using the
         * NotificationManager. I will not cover this here, go check the docs on
         * NotificationManager.
         * <p/>
         * What you HAVE to do is call stopSelf() after you've pushed your
         * notification(s). This will:
         * 1) Kill the service so it doesn't waste precious resources
         * 2) Call onDestroy() which will release the wake lock, so the device
         * can go to sleep again and save precious battery.
         */
        @Override
        protected void onPostExecute(OPMStatus opms) {
            // handle your data
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (!opms.txt.equals(prefs.getString("statusText", "")) && opms.status != MainActivity.Status.Open) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("statusText", opms.txt);
                editor.apply();

                Log.d("NotificationService", "About to issue notification, stored: " + prefs.getString("statusText", "") + " and retrieved: " + opms.txt);
                sendNotification(opms.status, opms.txt);
                stopSelf();
            } else {
                Log.d("NotificationService", "No notification needed");
                if (opms.status == MainActivity.Status.Open) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("statusText", opms.txt);
                    editor.apply();
                }
            }
        }

        private void sendNotification(MainActivity.Status status, String alert) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.ic_stat_action_report_problem)
                                .setContentTitle(status.toString())
                                .setContentText(alert);

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            mBuilder.setAutoCancel(true);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // notificationId allows you to update the notification later on.
            mNotificationManager.notify(MainActivity.NOTIFICATION_ID, mBuilder.build());
        }

    }
}