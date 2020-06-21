# AirDriveDownload

Download log data from [AirDrive Keylogger](http://www.keelog.com/hardware-keylogger/).

Main page: [heuberger.github.io/AirdDriveDownload](https://heuberger.github.io/AirdDriveDownload). 

This is an Eclipse workspace for Java version 8.

## Projects

### `gui`

Main program, needs one of the HTTP-Services (corresponding JAR file in same directory). The service is loaded using the [`ServiceLoader`](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) class.

### `http8`

HTTP-Service using the Java 8.

### `server`

An HTTP server to emulate the logger for developing/testing.

Documents
---

### Download Samples

* [LOG1.TXT](docs/data/LOG1.TXT) - page 1
* [LOG2.TXT](docs/data/LOG2.TXT) - page 2
* [LOG1-5.TXT](docs/data/LOG1-5.TXT) - pages 1-5

### HTTP Samples

* [headers.txt](docs/data/headers.txt) - request and reply headers
* [main.txt](docs/http/main.txt) - main page
* [first.txt](docs/http/first.txt) - log page 1
* [download.txt](docs/http/download.txt) - download page
* [action15.txt](docs/http/action15.txt) - download action pages 1-5

### Data Fields

* [fields.html](docs/data/fields.html) ([field.ods](docs/data/fields.ods))

