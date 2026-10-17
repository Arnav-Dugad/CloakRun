# Firebase setup (manual steps)

CloakRun runs entirely on the free **Spark** plan. These steps need a human in the Firebase console. They take about
15 minutes. Nothing here enables billing, and nothing should.

The app builds and runs without Firebase (offline mode). Firebase features (accounts, lobbies, matches) light up once
`app/google-services.json` is present.

## Values you will need

| Item | Value |
| --- | --- |
| Release package | `com.arnavgames.cloakrun` |
| Debug package | `com.arnavgames.cloakrun.debug` (debug builds install alongside release) |
| Release SHA-1 | `90:F8:66:9E:0F:3A:38:C0:97:A5:BA:5D:02:F9:FA:86:70:72:69:4B` |
| Release SHA-256 | `41:40:6A:81:F3:0E:5E:10:8E:1A:E5:9D:B6:05:A2:B6:6A:7D:4F:AE:DA:EB:C6:8F:BF:9A:90:CB:CD:FC:AA:72` |
| Debug SHA-1 (this machine) | `90:A3:FC:4A:BA:B3:DD:DE:DA:44:95:D5:F5:A1:DD:B3:0E:2A:A7:F5` |
| Debug SHA-256 (this machine) | `E7:83:3F:B0:28:BC:BF:40:00:CC:86:41:83:AC:FC:23:23:FA:18:CB:BB:73:22:32:66:D4:79:B3:2B:7F:E8:EA` |

Fingerprints are public certificate hashes, not secrets. Another machine's debug keystore has a different
fingerprint: run `keytool -list -v -keystore ~/.android/debug.keystore -storepass android` and add it too.

## 1. Create the project

1. Open https://console.firebase.google.com and click **Create a project**.
2. Name it `CloakRun`. Note the generated project ID (for example `cloakrun-1a2b3`).
3. Google Analytics: **enable** it and use or create a default account. Analytics is free.
4. Confirm the plan badge shows **Spark**. Do not upgrade.

## 2. Register the Android apps

Register **two** Android apps in the same project:

1. **Release:** package `com.arnavgames.cloakrun`, nickname `CloakRun`. Add the release SHA-1, then use
   **Project settings > Your apps > Add fingerprint** to add the release SHA-256.
2. **Debug:** package `com.arnavgames.cloakrun.debug`, nickname `CloakRun Debug`. Add the debug SHA-1 and SHA-256.
3. Download `google-services.json` after both apps exist. It then contains both clients. Save it as
   `app/google-services.json`. It is git-ignored, so never commit it.

## 3. Authentication

1. **Build > Authentication > Get started.**
2. **Sign-in method:** enable **Anonymous**.
3. Enable **Google**. Pick a support email. Save, then copy the **Web client ID** shown under the Google provider's
   "Web SDK configuration". The app needs it for Credential Manager (step 9).

## 4. Cloud Firestore

1. **Build > Firestore Database > Create database.**
2. Edition: **Standard**. Database ID: leave as `(default)`.
3. Location: pick the region closest to your players (for example `asia-south1` for India). This cannot be changed
   later.
4. Start in **production mode** (deny all). Our rules replace this in step 7.
5. Do not configure TTL policies. TTL is not free on Spark (see `QUOTA_BUDGET.md`).

## 5. Realtime Database

1. **Build > Realtime Database > Create database.**
2. Location: the same region family as Firestore if offered.
3. Start in **locked mode**.
4. Copy the database URL (for example `https://cloakrun-1a2b3-default-rtdb.asia-southeast1.firebasedatabase.app`).
   `google-services.json` usually contains it already. If you created the database after downloading the file,
   download it again.

## 6. App Check (monitor mode)

1. **Build > App Check > Apps.** Register both Android apps with **Play Integrity**.
2. In the Play Integrity settings for the release app, **turn off "Require PLAY_RECOGNIZED"**. CloakRun ships as a
   sideloaded APK from GitHub, which can never get that label.
3. **APIs tab: leave every product unenforced.** CloakRun runs App Check in monitor mode. Enforcement is switched
   on later from Remote Config (`appcheck_enforce`) after the metrics look healthy.
4. For debug builds: run the debug app once, copy the debug token from Logcat (search for `debug secret`), and add
   it under **App Check > Apps > CloakRun Debug > Manage debug tokens**.

## 7. Deploy Security Rules (wait for the go-ahead)

Rules are tested against the emulator in CI before every deploy. When the maintainer says the rules are ready:

```bash
npm install -g firebase-tools          # once (15.34.0 or newer)
firebase login
firebase use --add                     # pick the CloakRun project, alias: default
firebase deploy --only firestore:rules,firestore:indexes,database
```

Until Phase 3 the shipped rules deny everything, which is the safe default.

## 8. Restrict the API key

The key in `google-services.json` identifies the project. It is not a secret, but it should only work from CloakRun.

1. Open https://console.cloud.google.com/apis/credentials and select the project.
2. Open the key named **Android key (auto created by Firebase)**.
3. **Application restrictions: Android apps.** Add each package with its SHA-1:
   - `com.arnavgames.cloakrun` with the release SHA-1
   - `com.arnavgames.cloakrun.debug` with the debug SHA-1, plus one per developer machine
4. **API restrictions: Restrict key.** Select at least: Firebase Installations API, Firebase Remote Config API,
   Cloud Firestore API, Firebase Realtime Database API, Identity Toolkit API, Token Service API, Firebase App Check
   API, FCM Registration API, Firebase Crashlytics API, Google Analytics for Firebase (Firebase Management API is not
   needed). If a feature fails with `API_KEY_ANDROID_APP_BLOCKED` or `PERMISSION_DENIED` after this, add the API
   named in the error.

## 9. Remote Config defaults

Remote Config needs no console setup: the app ships in-app defaults. To tune live, publish parameters with these keys:
`matches_enabled`, `rtdb_enabled`, `rtdb_min_write_interval_ms`, `appcheck_enforce`, `map_style_url_night`,
`map_style_url_day`.

## 10. CI secrets

CI needs the same `google-services.json` to build Firebase-enabled releases:

```bash
base64 -w0 app/google-services.json | gh secret set GOOGLE_SERVICES_JSON_B64 --repo Arnav-Dugad/CloakRun
```

On Windows Git Bash, `base64 -w0` works as shown. Without this secret, CI builds the offline variant.

## Done checklist

- [ ] Project on Spark, Analytics on
- [ ] Two Android apps with SHA-1 and SHA-256 fingerprints
- [ ] Anonymous and Google sign-in enabled; Web client ID noted
- [ ] Firestore (Standard, `(default)`, production mode) created
- [ ] Realtime Database (locked) created
- [ ] App Check registered, PLAY_RECOGNIZED off, nothing enforced
- [ ] API key restricted
- [ ] `app/google-services.json` saved locally
- [ ] `GOOGLE_SERVICES_JSON_B64` secret set
- [ ] Rules deployed (only after the go-ahead)
