package os.juanamd.icystreamdata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IcyStreamDataRetriever {
	private static final int MAX_METADATA_LENGTH = 4080;
	private static final int HEADER_BYTE_LIMIT = 100000;

	private URL streamUrl;
	private int maxReadRetries;
	private URLConnection connection;
	private IcyStreamHeader headerData;
	private String metadata;

	public IcyStreamDataRetriever(URL url, int maxRetries) {
		streamUrl = url;
		maxReadRetries = maxRetries;
	}

	public IcyStreamHeader getHeaderData() {
		return headerData;
	}

	public String getMetadata() {
		return metadata;
	}

	synchronized public void retrieve() throws IOException, Exception {
		initConnection();
		updateHeaderData();
		if (headerData.metaInt > 0) updateMetadata();
		else throw new Exception("Could not retrieve data from stream");
		connection.getInputStream().close();
	}

	private void initConnection() throws IOException {
		connection = streamUrl.openConnection();
		connection.setRequestProperty("Icy-MetaData", "1");
		connection.setRequestProperty("Connection", "close");
		connection.setRequestProperty("Accept", null);
		connection.connect();
	}

	private void updateHeaderData() throws IOException {
		Map<String, List<String>> header = connection.getHeaderFields();
		headerData = new IcyStreamHeader();
		if (header.containsKey("icy-metaint")) {
			// Headers are sent via HTTP
			headerData.bitRate = parseInt(getParamFromHttpHeader(header, "icy-br"));
			headerData.genre = getParamFromHttpHeader(header, "icy-genre");
			headerData.name = getParamFromHttpHeader(header, "icy-name");
			headerData.url = urlDecode(getParamFromHttpHeader(header, "icy-url"));
			headerData.pub = parseInt(getParamFromHttpHeader(header, "icy-pub"));
			headerData.metaInt = parseInt(getParamFromHttpHeader(header, "icy-metaint"));
		} else {
			// Headers are sent within a stream
			String streamHeader = getStreamHeaderFields();
			if (streamHeader != null) {
				headerData.bitRate = parseInt(getParamFromStreamHeader(streamHeader, "icy-br"));
				headerData.genre = getParamFromStreamHeader(streamHeader, "icy-genre");
				headerData.name = getParamFromStreamHeader(streamHeader, "icy-name");
				headerData.url = urlDecode(getParamFromStreamHeader(streamHeader, "icy-url"));
				headerData.pub = parseInt(getParamFromStreamHeader(streamHeader, "icy-pub"));
				headerData.metaInt = parseInt(getParamFromStreamHeader(streamHeader, "icy-metaint"));
			}
		}
	}

	private String getParamFromHttpHeader(Map<String, List<String>> header, String paramName) {
		List<String> paramList = header.get(paramName);
		if (paramList != null && !paramList.isEmpty()) return paramList.get(0);
		return null;
	}

	private String urlDecode(String value) {
		try {
			return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
		} catch (Exception e) {
			return null;
		}
	}

	private int parseInt(String number) {
		try {
			return Integer.parseInt(number);
		} catch (Exception e) {
			return -1;
		}
	}

	private String getStreamHeaderFields() throws IOException {
		InputStream stream = connection.getInputStream();
		StringBuilder strHeaders = new StringBuilder();
		int count = 0;
		char c;
		while ((c = (char) stream.read()) != -1) {
			count++;
			if (count > HEADER_BYTE_LIMIT) return null;
			strHeaders.append(c);
			int len = strHeaders.length();
			if (len > 5 && (strHeaders.substring(len - 4, len).equals("\r\n\r\n"))) {
				// end of headers
				break;
			}
		}
		return strHeaders.toString();
	}

	private String getParamFromStreamHeader(String header, String paramName) {
		Pattern p = Pattern.compile("\\r\\n(" + paramName + "):\\s*(.*)\\r\\n");
		Matcher m = p.matcher(header);
		if (m.find()) return m.group(2);
		return null;
	}

	private void updateMetadata() throws IOException {
		// Metadata may be empty initially, try several times
		String rawMetadata = "";
		for (int i = 0; i < maxReadRetries; i++) {
			rawMetadata = readMetadata();
			if (isRawMetadataValid(rawMetadata)) {
				break;
			}
		}
		metadata = parseMetadata(rawMetadata, "StreamTitle='(.*?)';");
		if (metadata == null) metadata = parseMetadata(rawMetadata, "StreamTitle='(.*?)'");
		if (metadata == null) metadata = parseMetadata(rawMetadata, "StreamTitle='(.*?)");
	}

	private boolean isRawMetadataValid(String rawMetadata) {
		if (rawMetadata.isEmpty()) return false;
		if (headerData.name == null || headerData.name.isEmpty()) return true;
		if (rawMetadata.contains(headerData.name)) return false;
		return true;
	}

	private String readMetadata() throws IOException {
		int b;
		int count = 0;
		int metaDataOffset = headerData.metaInt;
		int metaDataLength = MAX_METADATA_LENGTH;
		boolean inData = false;
		List<Byte> metadataBytes = new ArrayList<Byte>();
		InputStream stream = connection.getInputStream();

		// Stream position should be either at the beginning or right after headers
		while ((b = stream.read()) != -1) {
			count++;
			if (count == metaDataOffset + 1) metaDataLength = b * 16;
			inData = count > metaDataOffset + 1 && count < (metaDataOffset + metaDataLength);
			if (inData && b != 0) metadataBytes.add((byte) b);
			if (count > (metaDataOffset + metaDataLength)) break;
		}

		return byteListToString(metadataBytes, StandardCharsets.UTF_8);
	}

	private String byteListToString(List<Byte> list, Charset charset) {
		if (list.size() == 0) return "";
		byte[] array = new byte[list.size()];
		int i = 0;
		for (Byte current : list) {
			array[i] = current;
			i++;
		}
		return new String(array, charset);
	}

	private String parseMetadata(String rawMetadata, String patternRegex) {
		Pattern p = Pattern.compile(patternRegex);
		Matcher m = p.matcher(rawMetadata);
		if (m.find()) return (String) m.group(1);
		return null;
	}
}
