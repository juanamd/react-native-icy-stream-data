package os.juanamd.icystreamdata;

import android.content.Context;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.net.URL;

public class RNIcyStreamDataModule extends ReactContextBaseJavaModule {
	private static final String TAG = "RNIcyStreamData";

	public RNIcyStreamDataModule(ReactApplicationContext reactContext) {
		super(reactContext);
	}

	@Override
	public String getName() {
		return TAG;
	}

	@ReactMethod
	public void getData(final String url, final Promise promise) {
		IcyStreamDataListener streamListener = new IcyStreamDataListener() {
			@Override
			public void onDataUpdated(IcyStreamHeader headerData, String artist, String trackname) {
				try {
					WritableMap headerMap = Arguments.createMap();
					headerMap.putInt("bitRate", headerData.bitRate);
					headerMap.putString("genre", headerData.genre);
					headerMap.putString("name", headerData.name);
					headerMap.putString("url", headerData.url);
					headerMap.putInt("pub", headerData.pub);
					WritableMap map = Arguments.createMap();
					map.putMap("header", headerMap);
					map.putString("artist", artist);
					map.putString("track", trackname);
					promise.resolve(map);
				} catch (Exception e) {
					promise.reject(e);
				}
			}
			public void onDataFail(Exception e) {
				promise.reject(e);
			}
		};
		try {
			IcyStreamData icyStreamData = new IcyStreamData(new URL(url), streamListener);
			icyStreamData.refreshData();
		} catch (Exception e) {
			promise.reject(e);
		}
	}
}
