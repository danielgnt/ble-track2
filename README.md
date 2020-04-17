### You can directly install the app-debug.apk or build the app yourself

### Import project ( You will need Android Studio )
This repository only contains the source, so you will need to create a project in Android Studio yourself.
Then copy in the soucre code, res folder and the AndroidManifest.xml

### Notes on when using the app
This is only a development and testing app, there are a lot of bugs. The following instructions will help in avoiding them.
* This app only runs on Android phones with Android API >= 26 .
* Before starting the app make sure bluetooth is enabled.
* When selecting power and latency do not press outside of the selection field! This will abort the selection screen and broadcasting will not commence.
* If you want to stop the app from broadcasting you need to terminate the app and then disable bluetooth! Otherwise even when terminated the app will keep advertising.
