// ITrackingServiceObserver.aidl
package jp.arakawa.hiroyuki.myfootprint;

// Declare any non-default types here with import statements

interface TrackingServiceObserver {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    //void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
    //        double aDouble, String aString);



    /**
    * Latitude 緯度
    * Longitude 軽度
    * Altitude 高度
    * Timestamp 時刻
    */
    void onLocationUpdate(double Latitude,double Longitude,double Altitude,String Timestamp);

}
