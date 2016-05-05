package jp.arakawa.hiroyuki.myfootprint;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polyline mPolyLine;
    final private String TAG = "MapsActivity";


    ServiceConnection mConnection;
    private Messenger mServiceMessenger;
    private Messenger mSelfMessenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);





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

        super.onDestroy();
    }

    private class ResponseHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
/*
                Snackbar.make(MainActivity.this.mBtnMap, "handle response=" + msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
*/
            switch (msg.what){
                case TrackingService.MSG_REGISTER_CLIENT:
                    break;
                case TrackingService.MSG_UNREGISTER_CLIENT:
                    break;

                case TrackingService.MSG_SET_VALUE:
                    //mMap.addPolyline(
                    //Log.d(TAG,"MSG_SET_VALUE");

                    Intent intent = (Intent)msg.obj;
                    double lat = intent.getDoubleExtra("Latitude",0);
                    double lon = intent.getDoubleExtra("Longitude",0);
                    LatLng currentloc = new LatLng(lat,lon);

                    List<LatLng> points = mPolyLine.getPoints();
                    points.add(currentloc);
                    mPolyLine.setPoints(points);

                    if (! mMap.isMyLocationEnabled()){
                        try {
                            mMap.setMyLocationEnabled(true);

                        }catch(SecurityException e){
                            e.printStackTrace();
                        }

                        CameraUpdate yourcamera =  CameraUpdateFactory.newLatLngZoom(currentloc,15);
                        mMap.animateCamera(yourcamera);
                    }else{
                        CameraUpdate yourcamera =  CameraUpdateFactory.newLatLngZoom(currentloc,mMap.getCameraPosition().zoom);
                        mMap.animateCamera(yourcamera);
                    }


                    break;
            }


            //Log.e(TAG, "handle response=" + msg);
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mPolyLine = mMap.addPolyline( new PolylineOptions() );

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        Log.d(TAG,"onMapReady");


        //サービスへの接続処理
        if (mSelfMessenger == null){

            mSelfMessenger = new Messenger(new ResponseHandler());
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.d(TAG,"onServiceConnected_Maps");
                    mServiceMessenger = new Messenger(service);

                    if (mServiceMessenger != null){
                        Message msg = Message.obtain(null, 1);
                        msg.what = TrackingService.MSG_REGISTER_CLIENT;
                        msg.replyTo = mSelfMessenger;
                        try{

                            mServiceMessenger.send(msg);
                            Log.d(TAG,"send MSG_REGISTER_CLIENT");
                        }catch(RemoteException e){
                            e.printStackTrace();
                        }
                    }else{
                        Log.d(TAG,"Could not send MSG_REGISTER_CLIENT");
                    }


                }
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG,"onServiceDisconnected_Maps");
                    MapsActivity.this.unbindService(mConnection);
                    mServiceMessenger = null;
                }
            };

            ComponentName myService = startService(new Intent(MapsActivity.this, TrackingService.class));
            bindService(new Intent(MapsActivity.this,TrackingService.class),
                    mConnection, Service.BIND_AUTO_CREATE);
        }


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("Lifecycle","onSaveInstanceState()");

        //インスタンスの保存


        //outState.putParcelableArray("Polyline", mPolyLine );
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Lifecycle","onRestoreInstanceState()");

        //インスタンスの復帰
    }
}
