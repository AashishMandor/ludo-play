# Build and test Ludo Play online (no Android Studio needed)

This project includes a GitHub Actions workflow that builds a debug APK in GitHub's cloud runner.

## 1. Upload the project to GitHub

1. Sign in to https://github.com and create a new repository, for example `ludo-play`.
2. Extract this ZIP on your computer.
3. Open the new empty repository on GitHub and choose **Add file → Upload files**.
4. Upload the *contents* of the `ludo_android` folder to the repository root. The root should show `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties`.
5. Commit the uploaded files to the `main` branch.

## 2. Add the cloud build workflow

If the `.github` folder was included during upload, skip this step. Otherwise:

1. In the repository, choose **Add file → Create new file**.
2. Enter this exact filename: `.github/workflows/android-build.yml`
3. Copy the complete contents of `android-build.yml` from this project into the editor.
4. Commit the new file to `main`.

## 3. Get the APK

1. Open the repository's **Actions** tab.
2. Select **Build Ludo Play APK**. It also runs automatically after a push to `main`.
3. Open the latest successful run.
4. Under **Artifacts**, download `ludo-play-debug-apk`.
5. Extract the downloaded artifact ZIP to get `app-debug.apk`.

## 4. Test the APK

- **In a browser:** Open https://appetize.io/upload, upload `app-debug.apk`, and launch the virtual Android device in the browser. Appetize requires an APK; this workflow creates it.
- **On an Android phone:** Transfer `app-debug.apk` to the phone, open it, and allow installation from that source if Android asks. This is a debug/testing build, not a Play Store release.
- **On a Windows PC:** You can use an Android emulator such as BlueStacks or LDPlayer, install the APK in it, and test with a mouse. These require installing the emulator locally.

## Notes

- This workflow builds the APK remotely; Android Studio and the Android SDK are not required on your computer.
- The first cloud build may take several minutes while dependencies and Android SDK components are downloaded.
- The project currently provides offline local multiplayer (2–4 players on one device). It does not include online matchmaking or internet multiplayer.
- The APK has not been compiled or runtime-tested in this environment. Check the GitHub Actions build result before installing it.
