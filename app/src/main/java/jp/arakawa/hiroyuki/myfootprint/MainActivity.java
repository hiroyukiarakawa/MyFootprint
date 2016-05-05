package jp.arakawa.hiroyuki.myfootprint;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    final private String TAG = "MainActivity";

    TextView mTxtAppStatus;

    FloatingActionButton mBtnRecord;
    FloatingActionButton mBtnMap;
    FloatingActionButton mBtnExport;

    ServiceConnection mConnection;

    private Messenger mServiceMessenger;
    private Messenger mSelfMessenger;

    private boolean mLogging;
/*
    private double prev_latitude;
    private double prev_longitude;
    private double mDistanceMoved;
*/
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSelfMessenger = new Messenger(new ResponseHandler());
        mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceMessenger = new Messenger(service);

                Message msg = Message.obtain(null, 1);
                msg.what = TrackingService.MSG_REGISTER_CLIENT;
                msg.replyTo = mSelfMessenger;
                try {
                    mServiceMessenger.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
                Log.d(TAG,"onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Message msg = Message.obtain(null, 1);
                msg.what = TrackingService.MSG_UNREGISTER_CLIENT;
                try {
                    mServiceMessenger.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }

                MainActivity.this.unbindService(mConnection);
                mServiceMessenger = null;
                Log.d(TAG,"onServiceDisconnected");
            }
        };

        ComponentName myService = startService(new Intent(MainActivity.this, TrackingService.class));
        bindService(new Intent(MainActivity.this,TrackingService.class),
                mConnection, Service.BIND_AUTO_CREATE);

        mTxtAppStatus = (TextView) findViewById(R.id.app_status);


        //GPXログ取得開始/終了
        mBtnRecord = (FloatingActionButton) findViewById(R.id.record);
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                if (mServiceMessenger != null) {
                    Log.d(TAG,"mServiceMessenger != null");
                    try {
                        if (!mLogging){
                            //移動距離測定用の変数初期化
                            //prev_latitude = 0;
                            //prev_longitude = 0;
                            //mDistanceMoved = 0;

                            Message msg = Message.obtain(null, 1);
                            msg.replyTo = mSelfMessenger;
                            msg.what = TrackingService.MSG_START_LOCATIONUPDATE;
                            mServiceMessenger.send(msg);
                            //mBtnMap.setEnabled(true);
                            //mBtnExport.setEnabled(true);
                            mTxtAppStatus.setText("AppStatus:Loggong....");
                        }else{
                            Message msg = Message.obtain(null, 1);
                            msg.what = TrackingService.MSG_STOP_LOCATIONUPDATE;
                            msg.replyTo = mSelfMessenger;
                            mServiceMessenger.send(msg);

                            //mBtnMap.setEnabled(false);
                            //mBtnExport.setEnabled(false);
                            mTxtAppStatus.setText("AppStatus:Stoping.");
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }





            }
        });

        mBtnMap = (FloatingActionButton) findViewById(R.id.map);
        //mBtnMap.setEnabled(false);
        mBtnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mLogging) {

                    Snackbar.make(view, "Please start logging", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                }else{
                    Intent intent = new Intent(MainActivity.this,MapsActivity.class);
                    startActivity(intent);
                }
                Log.d(TAG,"mBtnMap.setOnClickListener");

            }
        });


        mBtnExport = (FloatingActionButton) findViewById(R.id.export);
        //mBtnExport.setEnabled(false);
        mBtnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mLogging) {

                    Snackbar.make(view, "Please start logging", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                }else{
                    Message msg = Message.obtain(null, 1);
                    msg.what = TrackingService.MSG_PRINT_GPX_REQUEST;
                    msg.replyTo = mSelfMessenger;

                    try{
                        mServiceMessenger.send(msg);
                    }catch(RemoteException e){
                        e.printStackTrace();
                    }
                }
                Log.d(TAG,"mBtnExport.setOnClickListener");

            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (mServiceMessenger != null){
            Message msg = Message.obtain(null, 1);
            msg.what = TrackingService.MSG_UNREGISTER_CLIENT;
            msg.replyTo = mSelfMessenger;
            try{

                mServiceMessenger.send(msg);
                Log.d(TAG,"send MSG_UNREGISTER_CLIENT");
            }catch(RemoteException e){
                e.printStackTrace();
            }
            mServiceMessenger = null;
        }

        if (mConnection != null) {
            unbindService(mConnection);
        }
        if (!mLogging){
            stopService( new Intent(MainActivity.this, TrackingService.class) );

        }

        super.onDestroy();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://jp.arakawa.hiroyuki.myfootprint/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://jp.arakawa.hiroyuki.myfootprint/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private ProgressDialog mProgressDialog = null;

    private class ResponseHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
/*
                Snackbar.make(MainActivity.this.mBtnMap, "handle response=" + msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
*/
            Intent intent;//ローカル変数
            switch (msg.what){
                case TrackingService.MSG_REGISTER_CLIENT:
                    intent = (Intent)msg.obj;
                    boolean alreadylogging = intent.getBooleanExtra("AlreadyLogging",false);
                    if (alreadylogging){
                        Log.d(TAG,"AlreadyLogging");
                        mBtnRecord.setImageResource(android.R.drawable.ic_media_pause);
                        mLogging = true;

                    }
                    break;
                case TrackingService.MSG_START_LOCATIONUPDATE:
                    mBtnRecord.setImageResource(android.R.drawable.ic_media_pause);
                    mLogging = true;
                    break;
                case TrackingService.MSG_STOP_LOCATIONUPDATE:
                    mBtnRecord.setImageResource(android.R.drawable.ic_media_play);
                    mLogging = false;
                    break;

                case TrackingService.MSG_SET_VALUE:
                    intent = (Intent)msg.obj;
                    double lat = intent.getDoubleExtra("Latitude",0);
                    double lon = intent.getDoubleExtra("Longitude",0);
                    String time = intent.getStringExtra("Timestamp");
                    float moved = intent.getFloatExtra("Moved",0);
                    TextView txtLat = (TextView)findViewById(R.id.latitude);
                    if (txtLat != null) txtLat.setText( "Latitude:"+ Double.toString(lat) );
                    TextView txtLon = (TextView)findViewById(R.id.longitude);
                    if (txtLon != null) txtLon.setText( "Longitude:" + Double.toString(lon) );
                    TextView txtTime = (TextView)findViewById(R.id.time);
                    if (txtTime != null) txtTime.setText( "Time:"+time );
                    TextView txtDistance = (TextView) findViewById(R.id.distance);
                    if (txtDistance != null) txtDistance.setText("Moved:" + moved + "[m]" );
                    break;
                case TrackingService.MSG_PRINT_GPX_START:
                    mProgressDialog = ProgressDialog.show(MainActivity.this, "Printing GPX to file", "Started");
                    break;
                case TrackingService.MSG_PRINT_GPX_COMPLETE:
                    //intent = (Intent)msg.obj;

                    mProgressDialog.dismiss();

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Done")
                            .setMessage("File was printed, press ok to send it via email")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String gpx_fn = Environment.getExternalStorageDirectory() + "/myfootprint.gpx";
                                    dialog.cancel();

                                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHms");
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, "");
                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "GPX" + sdf1.format(new Date()) + ".gpx");
                                    emailIntent.setType("plain/text");
                                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Here's your file");
                                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + gpx_fn));

                                    startActivity(emailIntent);


                                }
                            })
                            .show();

                    break;
                case TrackingService.MSG_PRINT_GPX_ERROR:
                    intent = (Intent)msg.obj;
                    mProgressDialog.dismiss();

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage("An error occurred while printing: " + intent.getStringExtra("message"))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();

                    break;
            }


            //Log.e(TAG, "handle response=" + msg);
        }
    }

}
