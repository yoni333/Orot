# Publishing Orot to Google Play

Everything needed to get this app onto the Play Store, in order.
Nothing here has been done yet except the code changes in step 0.

Run all commands from the repository root:
`C:\Users\yehonatany\code\personal\Orot`

---

## 0. Code changes — DONE

Already applied to the repo (still uncommitted at the time of writing):

- `app/build.gradle.kts` — `applicationId` changed from `com.example` to
  `com.yoni.orot`. Play rejects any package name containing `com.example`.
  This ID is **permanent** once published and can never be changed.
- `.github/workflows/android.yml` — the release job now runs
  `:app:bundleRelease` and uploads the `.aab`, plus `mapping.txt` and the APK.
- `app/src/androidTest/.../ExampleInstrumentedTest.kt` — package assertion
  updated to match the new applicationId.
- `.gitignore` — keystore files excluded so they can never be committed.

Note: `namespace` is deliberately still `com.example`. It only controls where
`R` and `BuildConfig` are generated and is invisible to Play. Renaming it would
mean moving every Kotlin source file for no benefit.

---

## Steps 1-2, the fast way

A script does both of the next two sections for you. It prompts for your
password interactively, so the password is never written to disk, never
printed, and never visible to other processes:

```
powershell -ExecutionPolicy Bypass -File .\setup-signing.ps1
```

It needs a JDK (for `keytool`) and optionally the GitHub CLI (`gh`) to set the
secrets automatically. If either is missing it tells you exactly what to
install. Without `gh` it still creates the keystore and writes
`keystore.b64.txt` for you to paste into the web UI.

The two sections below are the same thing done by hand - read them to
understand what the script does, or follow them if you prefer manual control.

---

## 1. Create the upload keystore — DO THIS ONCE

This key identifies you as the publisher. **If you lose it you can never update
the app again** under the same listing.

```
keytool -genkeypair -v -keystore my-upload-key.jks -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 10000 -alias upload -dname "CN=Yoni, O=Orot, C=IL"
```

It prompts for a password twice. Pick a strong one and record it in a password
manager together with the alias (`upload`).

**Back up `my-upload-key.jks` somewhere off this machine** — password manager
attachment, encrypted cloud folder, USB drive. Not in git; `.gitignore` blocks it.

Verify it was created correctly:

```
keytool -list -v -keystore my-upload-key.jks -alias upload
```

---

## 2. Add the signing secrets to GitHub

Convert the keystore to base64 so it can live in a GitHub secret:

```
certutil -encode my-upload-key.jks keystore.b64.txt
```

Open `keystore.b64.txt`, delete the `-----BEGIN CERTIFICATE-----` and
`-----END CERTIFICATE-----` lines, and copy everything in between.

Go to **GitHub repo → Settings → Secrets and variables → Actions →
New repository secret** and add all four:

| Secret name       | Value                                      |
|-------------------|--------------------------------------------|
| `KEYSTORE_BASE64` | the base64 blob you just copied            |
| `STORE_PASSWORD`  | the keystore password from step 1          |
| `KEY_PASSWORD`    | same password (keytool uses one for both)  |
| `KEY_ALIAS`       | `upload`                                   |

Then delete the temporary file:

```
del keystore.b64.txt
```

If `KEYSTORE_BASE64` is missing, the workflow still builds — but it signs with a
throwaway key and names the artifact `app-release-aab-TEST-SIGNED-do-not-publish`.
That file is installable for testing but **cannot** be published.

---

## 3. Build the AAB

There is no Gradle wrapper committed and no Android SDK on this machine, so the
build runs in GitHub Actions rather than locally.

1. Commit and push to `main`.
2. Open the repo's **Actions** tab and wait for *Android Build & Test* to finish.
3. Open the completed run and download the **`app-release-aab`** artifact.
4. Unzip it — inside is `app-release.aab`. That is the file Play wants.

Also download **`app-release-mapping`** and keep it with that build. Play uses
`mapping.txt` to make crash reports readable.

Building locally instead (only if you install Android Studio + SDK):

```
gradle :app:bundleRelease
```

Output lands at `app\build\outputs\bundle\release\app-release.aab`.

---

## 4. Create the Play developer account

- Sign up at https://play.google.com/console
- **One-time fee: $25 USD**, paid by card.
- Identity verification is required and can take a few days. Personal accounts
  need a legal name and address that match your ID.

**Important for new personal accounts:** Google currently requires a closed test
with **at least 12 testers running the app for 14 continuous days** before you
can apply for production access. Plan for that timeline — recruit the testers
before you need them. Confirm the current rule in the Console, as Google adjusts it.

---

## 5. Listing assets you still need to produce

None of these exist in the repo yet.

- [ ] **App icon, 512×512 PNG.** The app currently ships the default Android
      Studio green-robot icon (`app/src/main/res/drawable/ic_launcher_foreground.xml`).
      Replace it with a real icon before publishing — reviewers and users both
      notice. Use Android Studio's *Image Asset* tool to regenerate the mipmaps.
- [ ] **Feature graphic, 1024×500 PNG/JPG.**
- [ ] **At least 2 phone screenshots** (up to 8). 16:9 or 9:16, each side between
      320px and 3840px.
- [ ] **Short description** — max 80 characters.
- [ ] **Full description** — max 4000 characters.
- [ ] **Privacy policy URL** — a publicly reachable page. Required. A GitHub
      Pages file is acceptable. The app requests `INTERNET` and
      `ACCESS_NETWORK_STATE`, so you must state what is and isn't collected.

---

## 6. Play Console forms to complete

- [ ] **Data safety** — declare what the app collects. This app stores notes,
      bookmarks and highlights locally in Room; answer honestly about whether
      anything leaves the device.
- [ ] **Content rating questionnaire** — religious text app, should rate broadly.
- [ ] **Target audience and content** — age groups.
- [ ] **Ads declaration** — no ads in this app.
- [ ] **Government apps / financial features** — both no.
- [ ] **App access** — declare that no login is required, so reviewers can open
      everything.

---

## 7. Upload

1. Play Console → **Create app** → name, default language, App/Game, Free/Paid.
2. **Release → Testing → Internal testing → Create new release.**
3. Let Google **enable Play App Signing** when prompted. Accept it. Your
   `my-upload-key.jks` stays the *upload* key; Google holds the *app signing*
   key and can help you recover if the upload key is ever lost.
4. Upload `app-release.aab`.
5. Upload `mapping.txt` under the same release if not attached automatically.
6. Write release notes, roll out to internal testing, and install on your own
   device to confirm it works when signed by Google.
7. Promote internal → closed (the 14-day / 12-tester run) → production.

---

## 8. Every release after the first

`versionCode` must increase on every single upload — Play rejects a repeat.
Edit `app/build.gradle.kts`:

```kotlin
versionCode = 2          // was 1 — bump on EVERY upload
versionName = "1.1"      // human-facing, your choice
```

Push, download the new `.aab` from Actions, upload to Play.

---

## Known gaps, not blockers

- **`isMinifyEnabled = false`** in `app/build.gradle.kts`. Legal to publish, just
  a larger download. It is off on purpose: enabling R8 without keep-rules risks
  breaking Room and Moshi, which resolve classes reflectively. Worth revisiting
  with proper `proguard-rules.pro` entries once the app is stable.
- **Default theme name** `Theme.MyApplication` and the unused purple/teal colors
  in `res/values/colors.xml` are template leftovers. Cosmetic and internal only.
- **`google-services.json` is absent.** Fine — the plugin is configured with
  `MissingGoogleServicesStrategy.WARN`, so the build only warns. Firebase
  features are all commented out in the dependency list.
