package os.juanamd.icystreamdata;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.URL;
import java.util.List;

public class IcyStreamData implements Runnable {
	private static final int METADATA_READ_MAX_RETRIES = 100;
	private static final String ARTIST_TRACK_DELIMITER = " - ";

	protected URL streamUrl;
	private String metadata;
	private IcyStreamHeader headerData;
	private OnStreamDataListener onStreamDataListener;

	public IcyStreamData(URL stream) {
		streamUrl = stream;
	}

	public IcyStreamData(URL stream, OnStreamDataListener listener) {
		streamUrl = stream;
		onStreamDataListener = listener;
	}

	public void setListener(OnStreamDataListener listener) {
		onStreamDataListener = listener;
	}

	public String getArtist() {
		if (metadata == null) return "";

		int end = metadata.indexOf(ARTIST_TRACK_DELIMITER);
		if (end == -1) return "";

		String artist = metadata.substring(0, end);
		return artist.trim();
	}

	protected String getTitle() {
		if (metadata == null) return "";

		int start = metadata.indexOf(ARTIST_TRACK_DELIMITER);
		if (start == -1) return metadata.trim();

		String title = metadata.substring(start + ARTIST_TRACK_DELIMITER.length());
		return title.trim();
	}

	public String getMetadata() {
		if (metadata == null) refreshData();
		return metadata;
	}

	public IcyStreamHeader getHeaderData() {
		if (headerData == null) refreshData();
		return headerData;
	}

	public void refreshData() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		Handler handler = new Handler(Looper.getMainLooper());
		try {
			IcyStreamDataRetriever dataRetriever = new IcyStreamDataRetriever(streamUrl, METADATA_READ_MAX_RETRIES);
			dataRetriever.retrieve();
			headerData = dataRetriever.getHeaderData();
			metadata = dataRetriever.getMetadata();
			final String title = getTitle();
			final String artist = getArtist();

			handler.post(new Runnable() {
				@Override
				public void run() {
					if (onStreamDataListener != null) onStreamDataListener.onDataUpdated(headerData, artist, title);
				}
			});

		} catch (final Exception e) {
			Log.e("IcyStreamData", "Exception while retrieving data", e);
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (onStreamDataListener != null) onStreamDataListener.onDataFail(e);
				}
			});
		}
	}

	public interface OnStreamDataListener {
		void onDataUpdated(IcyStreamHeader headerData, String artist, String trackname);

		void onDataFail(Exception e);
	}
}
