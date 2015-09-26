package com.soboapps.opm_status;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends ActionBarActivity {

    public static final String STATUS_URL = "http://www.opm.gov/json/operatingstatus.json";
    public static final int NOTIFICATION_ID = 493812;
    private static final String DEFAULT_POLLING_INTERVAL = "30";

    // Static form for access from broadcast receiver
    public static int getPersistentUpdateIntervalMins(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String minsStr = prefs.getString("updateIntervalMins", DEFAULT_POLLING_INTERVAL);
        return Integer.parseInt(minsStr);
    }

    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        listenRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
        // Wipe status of notification
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

        int minutes = getPersistentUpdateIntervalMins();
        Log.d("MainActivity", "Update interval is " + minutes);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, NotificationService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        am.cancel(pi);
        // by my own convention, minutes <= 0 means notifications are disabled
        if (minutes > 0) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + minutes * 60 * 1000,
                    minutes * 60 * 1000, pi);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intentSetPref = new Intent(getApplicationContext(), PrefActivity.class);
            startActivityForResult(intentSetPref, 0);
        }

        return super.onOptionsItemSelected(item);
    }

    // Executes a rest call to the url and updates visuals as well as persisted status
    void refreshStatus() {
        Log.d("MainActivity", "About to execute refresh");

        AsyncTask task = new AsyncTask() {
            @Override
            protected OPMStatus doInBackground(Object[] params) {
                return new OPMStatus(SyncRestClient.connect());
            }

            @Override
            protected void onPostExecute(final Object obj) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OPMStatus opms = (OPMStatus) obj;
                        setStatusTextLocation(opms.location);
                        setStatusTextAppliesTo(opms.appliesto);
                        setStatusTextPersistent(opms.txt);
                        setStatusImage(opms.status);
                        setStatusText(opms.location, opms.appliesto, opms.txt, opms.status);
                    }
                });
            }
        };
        task.execute();
    }


    // Attach a listener to the refresh button
    private void listenRefresh() {
        Button refreshButton = (Button) findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshStatus();
            }
        });
    }

    // Set shared preferences status location
    void setStatusTextLocation(String location) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("statusLocation", location);
        editor.apply();
    }

    // Set shared preferences status Applies To
    void setStatusTextAppliesTo(String appliesto) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("statusAppliesTo", appliesto);
        editor.apply();
    }

    // Set shared preferences status text
    void setStatusTextPersistent(String txt) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("statusText", txt);
        editor.apply();
    }

    // Get shared preferences update interval in minutes
    int getPersistentUpdateIntervalMins() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String minsStr = prefs.getString("updateIntervalMins", DEFAULT_POLLING_INTERVAL);
        return Integer.parseInt(minsStr);
    }

    // This function sets the text of the textviews
    private void setStatusText(String location, String appliesto, String txt, Status status) {
        //TextView statusTitle = (TextView) findViewById(R.id.statusTitle);
        TextView statusLocation = (TextView) findViewById(R.id.statusLocation);
        TextView statusAppliesTo = (TextView) findViewById(R.id.statusAppliesToTxt);
        TextView statusText = (TextView) findViewById(R.id.statusText);
        statusLocation.setText(location);
        statusAppliesTo.setText(" " + appliesto);
        statusText.setText(txt);
        TextView timeText = (TextView) findViewById(R.id.currentTime);
        TextView statusPhrase = (TextView) findViewById(R.id.statusPhrase);

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        Date date = new Date();
        timeText.setText(" " +  dateFormat.format(date));
        switch (status) {
            case Unknown:
                statusPhrase.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                break;
            case Open:
                statusPhrase.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case Alert:
                statusPhrase.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                break;
            default:
                statusPhrase.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        Log.d("Debug", status.toString());
        statusPhrase.setText(getString(R.string.status) + " " + status.toString());

    }

    // This function will set the icon based on the current status
    private void setStatusImage(Status s) {
        ImageView statusImage = (ImageView) findViewById(R.id.statusImage);
        int resId;
        switch (s) {
            case Unknown:
                resId = R.drawable.status_unknown;
                break;
            case Open:
                resId = R.drawable.status_open;
                break;
            case Alert:
                resId = R.drawable.status_alert;
                break;
            default:
                resId = R.drawable.status_closed;
        }
        statusImage.setImageResource(resId);
    }

    public enum Status {Unknown, Open, Alert, Closed}
}
