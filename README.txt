CARNET CT - ANDROID APP (APK)
=============================

What it is: the Carnet CT job log as a real Android app. Works with no internet, keeps jobs and
fiche technique photos in the phone's memory (inside the app). The app has no Internet permission.

BUILD THE APK ON YOUR COMPUTER (one time, about 15 minutes)
-----------------------------------------------------------
1) Install Android Studio (free): https://developer.android.com/studio
2) Unzip this folder. In Android Studio: File > Open > choose the folder "carnet-ct-android".
3) Wait for "Gradle sync" to finish (needs internet the first time; it downloads Android tools).
   If Android Studio asks to install an SDK platform or to update the Android Gradle Plugin, accept.
4) Menu: Build > Build Bundle(s) / APK(s) > Build APK(s).
5) When it finishes, click "locate" in the popup. The file is:
   app/build/outputs/apk/debug/app-debug.apk
6) Send app-debug.apk to your phone (USB cable, Google Drive, WhatsApp to yourself...).
7) On the phone: open the file and allow "Install unknown apps" for the app you opened it from.

Without Android Studio's menu you can also run, in this folder:   gradlew assembleDebug

OR BUILD THE APK ON GITHUB (no Android Studio, nothing to install)
-------------------------------------------------------------------
1) Create a free account at github.com, then New repository. Name: carnet-ct. Choose PRIVATE. Create.
2) Unzip this folder on your computer. In the new repository click "uploading an existing file".
   Drag ALL the contents of the folder "carnet-ct-android" into the page (the files and folders inside it,
   not the zip). Check that the folder ".github" went up too, then press Commit changes.
   If ".github" did not go up: Add file > Create new file, type the name
   .github/workflows/build-apk.yml   and paste the content of the file build-apk.yml. Commit.
3) Open the "Actions" tab. The workflow "Build APK" starts by itself (or choose it, then Run workflow).
   It takes about 5 to 10 minutes. A green check means it worked.
4) Click the finished run, scroll to "Artifacts", click CarnetCT-apk. A zip downloads; open it, it contains app-debug.apk.
5) Send app-debug.apk to your phone and install it (allow "Install unknown apps").
If the run turns red, open it, copy the error text from the red step and send it.
Both methods sign the app with the same key (app/carnetct.jks), so a new APK installs over the old one and keeps your data.
Keep the repository PRIVATE: the key is inside it.

USING IT
--------
- Entry tab: type the job. Take photo / From gallery for the fiche technique.
- Search tab: type a well name, see all its jobs and photos.
- Backup tab: Download backup makes a zip in Downloads/CarnetCT (jobs + photos).
  Do this regularly and copy the zip to Google Drive or your computer.
  Uninstalling the app or clearing its data deletes everything inside it. Restore backup brings it back.
- Excel export/import also saves into Downloads/CarnetCT.

IMPORTANT
---------
This project was written and checked without being able to compile it in the environment where it was made.
If Android Studio shows an error, copy the error text and send it, it is usually a one-line fix.
The web part (app/src/main/assets/index.html) was tested in a browser with a simulated Android bridge.
Updating later: build a new APK and install it over the old one (same package name and same key keep your data).
Do not uninstall first.
