# AirDriveDownload

Download log data from the [AirDrive Keylogger](http://www.keelog.com/hardware-keylogger/).

The logger's user interface only allows downloading a file with a maximum of 5 pages with 2kB each. The downloaded files must also be joined manually. To This application connects to the logger and download all (selected) pages to one file.

Main page: https://heuberger.github.io/AirdDriveDownload 

-----

## Development

This is an Eclipse workspace for Java version 8.

### `gui`

Main program, needs one of the HTTP-Services (corresponding JAR file in same directory). The service is loaded using the [`ServiceLoader`](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) class.

### `http8`

HTTP-Service using the Java 8.

### `server`

An HTTP server to emulate the logger for testing.

