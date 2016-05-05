package jp.arakawa.hiroyuki.myfootprint;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.location.Location;
import android.widget.Toast;

import com.codebutchery.androidgpx.print.GPXFilePrinter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import com.codebutchery.androidgpx.data.*;


public class TrackingService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GPXFilePrinter.GPXFilePrinterListener{
    private final String TAG = "TrackingService";
    //Activity <--> Serviceとやり取りで指定するwhat
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_VALUE = 3;
    public static final int MSG_START_LOCATIONUPDATE = 4;
    public static final int MSG_STOP_LOCATIONUPDATE = 5;

    public static final int MSG_PRINT_GPX_REQUEST = 6;
    public static final int MSG_PRINT_GPX_START = 7;
    public static final int MSG_PRINT_GPX_COMPLETE = 8;
    public static final int MSG_PRINT_GPX_ERROR = 9;

    private Messenger mServiceMessenger;
    /** 登録されたクライアントのメッセンジャーを保持するリスト */
    ArrayList<Messenger> mClients = new ArrayList();
    int mValue = 0;

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;
    protected Location mPreLocation;
    protected float mMovedDistance;
    //protected String mLastUpdateTime;
    protected Boolean mRequestingLocationUpdates;


    protected Location mPreLocation_GPX;//3m以上移動したらGPXに保存
    private GPXDocument mGPXDocument;


    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //createLocationRequest();
    }

    protected void createLocationRequest() {
        Log.d(TAG, "createLocationRequest");
        if (mGoogleApiClient == null){
            Log.d(TAG,"mGoogleApiClient == null");
            buildGoogleApiClient();
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


/*
http://qiita.com/Helmos/items/5260601711560d9bc862
    Toast.makeText(this, "onBind", Toast.LENGTH_SHORT).show();
    // 通知押下時に、MusicPlayServiceのonStartCommandを呼び出すためのintent
    Intent notificationIntent = new Intent(this, MusicPlayService.class);
    PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
    // サービスを永続化するために、通知を作成する
    NotificationCompat.Builder builder = new　NotificationCompat.Builder(getApplicationContext());
    builder.setContentIntent(pendingIntent);
    builder.setTicker("準備中");
    builder.setContentTitle("title");
    builder.setContentText("text");
    builder.setSmallIcon(android.R.drawable.ic_dialog_info);
    mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mNM.notify(R.string.service_music_play, builder.build());
    // サービス永続化
    startForeground(R.string.service_music_play, builder.build());

//stopForegroundを別途呼ぶ必要あり。
* */

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        mCurrentLocation = null;
        mPreLocation = null;
        mMovedDistance = 0;

        if (mLocationRequest == null){
            createLocationRequest();
        }

        try {

            Log.d(TAG, "startLocationUpdates_2");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            mRequestingLocationUpdates = true;
        }catch(SecurityException e){
            e.printStackTrace();

        }
        //サービスの永続化
        // 通知押下時に、MusicPlayServiceのonStartCommandを呼び出すためのintent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        // サービスを永続化するために、通知を作成する
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentIntent(pendingIntent);
        builder.setTicker(getResources().getString(R.string.notification_logging));
        builder.setContentTitle( getResources().getString(R.string.app_name) );
        builder.setContentText( getResources().getString(R.string.notification_logging));
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNM.notify(R.string.notification_logging, builder.build());
        // サービス永続化
        startForeground(R.string.notification_logging, builder.build());

        Toast.makeText(this, R.string.toast_startlogging, Toast.LENGTH_SHORT).show();
    }

    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mRequestingLocationUpdates = false;

        //サービスの永続化を解除
        stopForeground(true);//引数はnotificationを消去するかどうか、らしい

        Toast.makeText(this, R.string.toast_stoplogging, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");


        if (mCurrentLocation == null) {
            Log.d(TAG,"onConnected. CurrentLocation == null#1");
            try{
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }catch(SecurityException e){
                e.printStackTrace();
                //this.finish();
            }
            Log.d(TAG,"onConnected. CurrentLocation == null#2");
            //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            //mCurrentLocation.getTime()
            //DoUpateClients();
        }

/*
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
*/
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {


        mCurrentLocation = location;
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //DoUpateClients();
        Intent intent = new Intent();
        intent.putExtra("Latitude",mCurrentLocation.getLatitude());
        intent.putExtra("Longitude",mCurrentLocation.getLongitude());
        intent.putExtra("Accuracy",mCurrentLocation.getAccuracy());
        intent.putExtra("Timestamp",DateFormat.getTimeInstance().format(new Date(mCurrentLocation.getTime())));
        if (mPreLocation == null){
            intent.putExtra("Moved",(float)0);
            mPreLocation = mCurrentLocation;
        }else{
            float[] result = new float[3];
            Location.distanceBetween(mPreLocation.getLatitude(),mPreLocation.getLongitude(),
                    mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),result);
            mMovedDistance = mMovedDistance + result[0];
            intent.putExtra("Moved",mMovedDistance);
            mPreLocation = mCurrentLocation;
        }


        Message msg = Message.obtain(null,MSG_SET_VALUE,intent);
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //GPX
        try{
            if (mPreLocation_GPX == null){
                GPXTrackPoint trkpt = new GPXTrackPoint((float)mCurrentLocation.getLatitude(),(float)mCurrentLocation.getLongitude());
                trkpt.setTimeStamp( new Date(mCurrentLocation.getTime()) );
                mGPXDocument.getTracks().get(0).getSegments().get(0).addPoint(trkpt);
                mPreLocation_GPX = mCurrentLocation;

            }else{
                float[] result = new float[3];
                Location.distanceBetween(mPreLocation_GPX.getLatitude(),mPreLocation_GPX.getLongitude(),
                        mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),result);
                if (result[0] >= 3){
                    GPXTrackPoint trkpt = new GPXTrackPoint((float)mCurrentLocation.getLatitude(),(float)mCurrentLocation.getLongitude());
                    trkpt.setTimeStamp( new Date(mCurrentLocation.getTime()) );
                    mGPXDocument.getTracks().get(0).getSegments().get(0).addPoint(trkpt);
                    mPreLocation_GPX = mCurrentLocation;
                }
            }

        }catch (Exception e){
            e.printStackTrace();

        }

        //Toast.makeText(this, "update",
        //        Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"TrackingService.onCreate");
        buildGoogleApiClient();
        if (mGoogleApiClient != null){
            mGoogleApiClient.connect();
        }

        mServiceMessenger = new Messenger(new RequestHandler());

        //GPX
        mGPXDocument = new GPXDocument(new ArrayList<GPXWayPoint>(),new ArrayList<GPXTrack>(),new ArrayList<GPXRoute>());
        mGPXDocument.getTracks().add(new GPXTrack());
        mGPXDocument.getTracks().get(0).addSegment(new GPXSegment());
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        if ((mGoogleApiClient != null)&&(mGoogleApiClient.isConnected())){
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {


        return mServiceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    private class RequestHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    Log.v("MESSENGER_SERVICE", "MSG_REGISTER_CLIENT:replyTo=" + msg.replyTo);
                    // replyToのメッセンジャーをリストに追加する
                    mClients.add(msg.replyTo);
                    if (msg.replyTo != null) {
                        try {
                            Message res = Message.obtain();
                            res.what = msg.what;//MSG_REGISTER_CLIENT
                            res.replyTo = mServiceMessenger;

                            //サービス永続化に伴い、MSG_REGISTER_CLIENTの段階ですでにserviceが走ってるcaseもある
                            Intent intent = new Intent();
                            intent.putExtra("AlreadyLogging",mRequestingLocationUpdates);
                            res.obj = intent;

                            msg.replyTo.send(res); // send response.
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.v("MESSENGER_SERVICE", "MSG_UNREGISTER_CLIENT:replyTo=" + msg.replyTo);
                    // replyToのメッセンジャーをリストから削除する
                    mClients.remove(msg.replyTo);
                    if (msg.replyTo != null) {
                        try {
                            Message res = Message.obtain();
                            res.what = msg.what;//MSG_UNREGISTER_CLIENT
                            res.replyTo = mServiceMessenger;
                            msg.replyTo.send(res); // send response.
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case MSG_SET_VALUE:
                    Log.v("MESSENGER_SERVICE", "MSG_SET_VALUE:NumOfClient="+mClients.size()+",replyTo=" + msg.replyTo );
                    // 送られてきた値を登録されているすべてのクライアントに送る
                    mValue = msg.arg1;

                    Message msg_data = Message.obtain(null, MSG_SET_VALUE, msg.obj);
                    for (int i = mClients.size() - 1; i >= 0; i--) {
                        try {
                            mClients.get(i).send(msg_data);
                        } catch (RemoteException e) {
                            // クライアントが死んでいる。それをリストから除去する。
                            // ループの内側でしても安全なようにリストを後ろから前に通過する。
                            mClients.remove(i);
                        }
                    }
                    break;
                case MSG_START_LOCATIONUPDATE:
                    Log.v("MESSENGER_SERVICE", "MSG_START_LOCATIONUPDATE:replyTo=" + msg.replyTo);
                    startLocationUpdates();
                    if ((msg.replyTo != null)&&(msg.replyTo != mServiceMessenger)) {
                        try {
                            Message res = Message.obtain();
                            res.what = msg.what;//MSG_START_LOCATIONUPDATE
                            res.replyTo = mServiceMessenger;
                            msg.replyTo.send(res); // send response.
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_STOP_LOCATIONUPDATE:
                    Log.v("MESSENGER_SERVICE", "MSG_STOP_LOCATIONUPDATE:replyTo=" + msg.replyTo);
                    stopLocationUpdates();
                    if ((msg.replyTo != null)&&(msg.replyTo != mServiceMessenger)) {
                        try {
                            Message res = Message.obtain();
                            res.what = msg.what;//MSG_STOP_LOCATIONUPDATE
                            res.replyTo = mServiceMessenger;
                            msg.replyTo.send(res); // send response.
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case MSG_PRINT_GPX_REQUEST:
                //GPXファイルへの出力
                    String gpx_fn = Environment.getExternalStorageDirectory() + "/myfootprint.gpx";
                    new GPXFilePrinter(mGPXDocument, gpx_fn, TrackingService.this).print();


                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onGPXPrintStarted() {
        Intent intent = new Intent();
        intent.putExtra("filename", Environment.getExternalStorageDirectory() + "/myfootprint.gpx");

        Message msg_data = Message.obtain(null, MSG_PRINT_GPX_START, intent);
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(msg_data);
            } catch (RemoteException e) {
                // クライアントが死んでいる。それをリストから除去する。
                // ループの内側でしても安全なようにリストを後ろから前に通過する。
                mClients.remove(i);
            }
        }
    }

    @Override
    public void onGPXPrintCompleted() {
        Intent intent = new Intent();
        intent.putExtra("filename",Environment.getExternalStorageDirectory() + "/myfootprint.gpx");
        Message msg_data = Message.obtain(null, MSG_PRINT_GPX_COMPLETE, intent);
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(msg_data);
            } catch (RemoteException e) {
                // クライアントが死んでいる。それをリストから除去する。
                // ループの内側でしても安全なようにリストを後ろから前に通過する。
                mClients.remove(i);
            }
        }
    }

    @Override
    public void onGPXPrintError(String message) {
        Intent intent = new Intent();
        intent.putExtra("filename",Environment.getExternalStorageDirectory() + "/myfootprint.gpx");
        intent.putExtra("message",message);
        Message msg_data = Message.obtain(null, MSG_PRINT_GPX_ERROR, intent);
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(msg_data);
            } catch (RemoteException e) {
                // クライアントが死んでいる。それをリストから除去する。
                // ループの内側でしても安全なようにリストを後ろから前に通過する。
                mClients.remove(i);
            }
        }
    }


}
