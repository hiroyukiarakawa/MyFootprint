// BindTrackingService.aidl
package jp.arakawa.hiroyuki.myfootprint;

import jp.arakawa.hiroyuki.myfootprint.TrackingServiceObserver;

interface BindTrackingService {

	String getString();

	//AIDL fileで定義されるmethodの引数、戻り値に指定出来るのはprimitive型、String、
	//List、Map、CharSequence、その他のAIDLに定義されたinterface、Parcelable interfaceを実装したclass
	//TrackingServiceObserverはAIDLで定義されているのでOK
	void setObserver(TrackingServiceObserver observer);
	void removeObserver(TrackingServiceObserver observer);

}
