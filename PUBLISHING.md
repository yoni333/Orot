# Publishing Orot to Google Play

Everything needed to get this app onto the Play Store, in order.
Nothing here has been done yet except the code changes in step 0.

Run all commands from the repository root:
`C:\Users\yehonatany\code\personal\Orot`

---

## 0. Code changes — DONE

Already applied to the repo and pushed:

- `app/build.gradle.kts` — `applicationId` changed from `com.example` to
  `com.yoni.orot`. Play rejects any package name containing `com.example`.
  This ID is **permanent** once published and can never be changed.
- `.github/workflows/android.yml` — the release job now runs
  `:app:bundleRelease` and uploads the `.aab`, plus `mapping.txt` and the APK.
- `app/src/androidTest/.../ExampleInstrumentedTest.kt` — package assertion
  updated to match the new applicationId.
- `.gitignore` — keystore files excluded so they can never be committed.
- `docker-compose.yml` + `docker/` — a containerised `keytool` so no JDK or
  Android SDK has to be installed on your machine.

Note: `namespace` is deliberately still `com.example`. It only controls where
`R` and `BuildConfig` are generated and is invisible to Play. Renaming it would
mean moving every Kotlin source file for no benefit.

---

## 1. Create the upload keystore — DO THIS ONCE, WITH DOCKER

This key identifies you as the publisher. **If you lose it you can never update
the app again** under the same listing.

Nothing needs installing on your machine — a throwaway JDK container does it.
Start Docker Desktop, then from the repo root:

```
docker compose run --rm keystore
```

It prompts for a password twice, then writes two files into the repo root:

| File               | What it is                                          |
|--------------------|-----------------------------------------------------|
| `my-upload-key.jks`| your signing key — **back this up off this machine** |
| `keystore.b64.txt` | the same key, base64 encoded, ready for GitHub       |

Both are gitignored and can never be committed. The container is deleted the
moment it exits (`--rm`); the key survives only because the repo folder is
bind-mounted into it.

Record the password in your password manager together with the alias
(`upload`). Neither Google nor anyone else can recover it for you.

The script refuses to run if `my-upload-key.jks` already exists, so it is safe
to re-run by mistake.

---

## 2. Add the signing secrets to GitHub

Open `keystore.b64.txt` and copy the whole thing — it is a single long line
with no header or footer to strip.

Go to **https://github.com/yoni333/Orot/settings/secrets/actions** →
**New repository secret**, and add all four:

| Secret name       | Value                                      |
|-------------------|--------------------------------------------|
| `KEYSTORE_BASE64` | the entire contents of `keystore.b64.txt`  |
| `STORE_PASSWORD`  | the password you chose in step 1           |
| `KEY_PASSWORD`    | the same password                          |
| `KEY_ALIAS`       | `upload`                                   |

Then delete the temporary file — the key still lives in `my-upload-key.jks`
and in your backup:

```
del keystore.b64.txt
```

If `KEYSTORE_BASE64` is missing, the workflow still builds — but it signs with a
throwaway key and names the artifact `app-release-aab-TEST-SIGNED-do-not-publish`.
That file is installable for testing but **cannot** be published.

---

## 3. Build the AAB — in GitHub

This is the normal path. Nothing is built on your machine.

1. Push to `main` (or use **Actions → Android Build & Test → Run workflow**).
2. Wait for the run to finish at https://github.com/yoni333/Orot/actions
3. Download the **`app-release-aab`** artifact from the finished run.
4. Unzip it — inside is `app-release.aab`. That is the file Play wants.

Also download **`app-release-mapping`** and keep it with that build. Play uses
`mapping.txt` to make crash reports readable.

### Building locally instead (optional, rarely needed)

Only if you are offline or debugging the build itself. This downloads the
Android SDK into a ~3 GB image on first use and takes several minutes:

```
$env:STORE_PASSWORD="your-password"
$env:KEY_PASSWORD=$env:STORE_PASSWORD
docker compose --profile local-build run --rm build
```

Output lands at `appuild\outputsundle
eleasepp-release.aab`.
The Gradle cache persists in a Docker volume, so later runs are much faster.

Run the unit tests the same way:

```
docker compose --profile local-build run --rm test
```

To just check that the code still compiles - no packaging, no signing, no
keystore - use the `verify` service. It is the cheapest way to catch a broken
refactor before pushing to Actions:

```
docker compose --profile local-build run --rm verify
```

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

## 5. Listing assets

- [x] **App icon — done.** Generated from `docs/הרב קוק סקיצה.jpg` (the pen-and-ink
      portrait of Rav Kook). The paper is knocked out to alpha via a luminance→alpha
      colour matrix, so the source card's rounded border and drop shadow are gone and
      only the ink remains; the paper tone comes from the background layer instead.
      - `docs/icon/play-icon-512.png` — 512×512, the file Play wants (173 KB).
      - `app/src/main/res/drawable-*dpi/ic_launcher_foreground.png` — adaptive
        foreground, art at 64% of the 108dp canvas so it stays inside the 72dp safe zone.
      - `app/src/main/res/drawable/ic_launcher_background.xml` — solid `#FBF9F1`.
      - `app/src/main/res/mipmap-*dpi/ic_launcher.png` + `ic_launcher_round.png` —
        legacy rasters for API 24–25. The old green-robot `.webp` files were deleted;
        leaving them alongside same-named `.png` would be a duplicate-resource error.
      - `<monochrome>` was removed from the adaptive-icon XML on purpose: a hatched
        portrait has no readable single-colour silhouette, and the themed-mode render
        came out as a muddy blob. Themed mode now falls back to the full-colour icon.
      - `docs/icon/orot-icon.svg` is the earlier flame draft, superseded and unused.
      Regenerate with `pwsh docs/icon/make-icons.ps1` (run from the repo root) if the
      source sketch changes. It needs Windows + System.Drawing; no SDK or Docker.
- [x] **Feature graphic — done.** `docs/icon/feature-graphic-1024x500.png`,
      exactly 1024×500 PNG. Sage vertical gradient, the portrait in a paper
      medallion on the left (deliberately echoing the launcher icon), Hebrew type
      right-aligned: "אורות" / "הראי״ה קוק זצ״ל" / a features line. Rendered at 2x
      and downsampled so the type stays crisp. Content is kept ~11% clear of every
      edge, because Play re-crops this asset to different aspect ratios per surface.
      Regenerate with `pwsh docs/icon/make-feature-graphic.ps1` from the repo root.
- [ ] **At least 2 phone screenshots** (up to 8). 16:9 or 9:16, each side between
      320px and 3840px.
- [x] **Short description** — see `docs/play-listing.md` (75/80 chars).
- [x] **Full description** — see `docs/play-listing.md` (997/4000 chars).
- [x] **Privacy policy page** written at `docs/privacy-policy.html` (Hebrew + English).
      **Still to do:** publish it. GitHub repo → Settings → Pages → Deploy from a
      branch → `main` / `/docs`. It then serves at
      https://yoni333.github.io/Orot/privacy-policy.html — that is the URL Play wants.
      Verify it loads in a private browser window before submitting; Play rejects
      unreachable policy URLs.

---

## 6. Play Console forms to complete

- [ ] **Data safety** — answer **"no data collected"**. The full text loads from
      `app/src/main/assets/orot_data.json`; notes, bookmarks, highlights and recent
      searches live only in the local Room database. There are no network calls at
      all — `INTERNET` and `ACCESS_NETWORK_STATE` are declared but unused. Draft
      answers are in `docs/play-listing.md`.
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

`versionCode` must increase on every single upload — Play rejects a repeat. You do
not bump it by hand: `app/build.gradle.kts` derives it from `GITHUB_RUN_NUMBER`, so
every Actions run produces a higher one than the last.

```kotlin
val ciVersionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.plus(1)
versionCode = ciVersionCode ?: 1     // CI: run number + 1. Local: always 1.
versionName = "1.1"                  // human-facing, bump this by hand
```

The `versionCode` of any given `.aab` is the run number shown beside the workflow in
the Actions UI (`#42`), plus one. A locally built `.aab` always carries `1` and is not
publishable — use the CI artifact.

So each release is: bump `versionName` if you want a new user-facing number, push,
download the new `.aab` from Actions, upload to Play.

One thing to watch: `run_number` is counted per workflow *name*. Renaming the workflow
in `.github/workflows/android.yml` restarts it at 1 and would drop `versionCode` below
what is already published. If that happens, raise the offset in `app/build.gradle.kts`
above the highest code you have uploaded.

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
