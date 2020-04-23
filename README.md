# react-native-icy-stream-data
Get radio stream ICY metadata and headers
***Only Android is supported for now***
Based on **[PsyRadioAndroid](https://github.com/le0pard/PsyRadioAndroid)**.

## Install
    yarn add https://github.com/juanamd/react-native-icy-stream-data.git

## Usage
```typescript
import IcyData from "react-native-icy-stream-data";

const url = "http://tuner.classical102.com";
const data = await IcyData.getData(url);
if (data) {
	const { artist, track, header) = data;
	console.log(artist, track);
	// ICY header fields: https://cast.readme.io/docs/icy
	console.log(header.bitRate, header.genre, header.name, header.pub, header.url);
}
```