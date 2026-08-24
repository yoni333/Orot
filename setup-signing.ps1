# setup-signing.ps1 - Steps 1 and 2 of PUBLISHING.md, automated.
#
# Creates the Play upload keystore and pushes the four signing secrets to
# GitHub Actions. Run it once, from the repository root:
#
#     powershell -ExecutionPolicy Bypass -File .\setup-signing.ps1
#
# Your password is read interactively and is never written to disk, never
# printed, and never passed on a command line where other processes could
# see it. Only you ever know it.

$ErrorActionPreference = 'Stop'

$KeystoreFile = 'my-upload-key.jks'
$Alias        = 'upload'

Write-Host ''
Write-Host '=== Orot - Play upload signing setup ===' -ForegroundColor Cyan
Write-Host ''

# --- Preflight -------------------------------------------------------------

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
  # Android Studio bundles a JDK; use it if the user has Studio but no PATH entry.
  $candidates = @(
    "$env:ProgramFiles\Android\Android Studio\jbr\bin\keytool.exe",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe"
  )
  foreach ($c in $candidates) {
    if (Test-Path $c) { $keytool = $c; break }
  }
}
if (-not $keytool) {
  Write-Host 'keytool was not found. It ships with any JDK.' -ForegroundColor Red
  Write-Host ''
  Write-Host 'Install one of these, reopen the terminal, and run this script again:'
  Write-Host '  - Microsoft OpenJDK 21:  https://learn.microsoft.com/java/openjdk/download'
  Write-Host '  - Eclipse Temurin 21:    https://adoptium.net/temurin/releases/'
  Write-Host '  - Android Studio:        https://developer.android.com/studio  (bundles a JDK)'
  exit 1
}
$keytoolPath = if ($keytool -is [string]) { $keytool } else { $keytool.Source }
Write-Host "keytool: $keytoolPath" -ForegroundColor DarkGray

$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
  Write-Host ''
  Write-Host 'GitHub CLI (gh) was not found.' -ForegroundColor Yellow
  Write-Host 'The keystore will still be created, but the secrets must be added by hand.'
  Write-Host 'Install from https://cli.github.com/ to have this script do it for you.'
  Write-Host ''
}

if (Test-Path $KeystoreFile) {
  Write-Host ''
  Write-Host "$KeystoreFile already exists." -ForegroundColor Yellow
  Write-Host 'Refusing to overwrite it. If you replace a keystore that has already'
  Write-Host 'published an app, you permanently lose the ability to update that app.'
  Write-Host 'Delete it manually only if you are certain it was never used.'
  exit 1
}

# --- Password --------------------------------------------------------------

Write-Host 'Choose a password for the keystore.' -ForegroundColor Cyan
Write-Host 'Save it in your password manager NOW - it cannot be recovered.'
Write-Host ''

$pw1 = Read-Host 'Password' -AsSecureString
$pw2 = Read-Host 'Confirm password' -AsSecureString

$b1 = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pw1)
$b2 = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pw2)
try {
  $plain1 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b1)
  $plain2 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b2)

  if ($plain1 -ne $plain2)   { Write-Host 'Passwords do not match.' -ForegroundColor Red; exit 1 }
  if ($plain1.Length -lt 6)  { Write-Host 'Password must be at least 6 characters (keytool requires it).' -ForegroundColor Red; exit 1 }

  # --- Generate keystore ---------------------------------------------------
  # -storepass:env keeps the password out of the process command line, where
  # any other user on the machine could read it from the process list.
  $env:OROT_KS_PW = $plain1

  Write-Host ''
  Write-Host 'Generating keystore...' -ForegroundColor Cyan

  & $keytoolPath -genkeypair -v `
    -keystore $KeystoreFile `
    -storetype PKCS12 `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -alias $Alias `
    -dname 'CN=Yoni, O=Orot, C=IL' `
    -storepass:env OROT_KS_PW `
    -keypass:env OROT_KS_PW

  if ($LASTEXITCODE -ne 0) { Write-Host 'keytool failed.' -ForegroundColor Red; exit 1 }

  & $keytoolPath -list -keystore $KeystoreFile -alias $Alias -storepass:env OROT_KS_PW | Out-Null
  if ($LASTEXITCODE -ne 0) { Write-Host 'Keystore verification failed.' -ForegroundColor Red; exit 1 }

  Write-Host "Created and verified $KeystoreFile" -ForegroundColor Green

  # --- Push secrets to GitHub ----------------------------------------------
  if ($gh) {
    & gh auth status 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
      Write-Host ''
      Write-Host 'gh is installed but not signed in. Run:  gh auth login' -ForegroundColor Yellow
      Write-Host 'Then re-run this script - it will skip keystore creation and just set secrets.'
    } else {
      Write-Host ''
      Write-Host 'Uploading secrets to GitHub Actions...' -ForegroundColor Cyan

      $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $KeystoreFile)))

      $b64     | & gh secret set KEYSTORE_BASE64
      $plain1  | & gh secret set STORE_PASSWORD
      $plain1  | & gh secret set KEY_PASSWORD
      $Alias   | & gh secret set KEY_ALIAS

      $b64 = $null
      Write-Host 'Secrets set: KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD, KEY_ALIAS' -ForegroundColor Green
    }
  } else {
    # No gh - write the blob out so it can be pasted into the web UI.
    [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $KeystoreFile))) |
      Set-Content -Path 'keystore.b64.txt' -Encoding ascii
    Write-Host ''
    Write-Host 'Wrote keystore.b64.txt' -ForegroundColor Yellow
    Write-Host 'Paste its contents into the KEYSTORE_BASE64 secret at:'
    Write-Host '  https://github.com/yoni333/Orot/settings/secrets/actions'
    Write-Host 'Also add STORE_PASSWORD and KEY_PASSWORD (your password) and KEY_ALIAS (upload).'
    Write-Host 'Delete keystore.b64.txt once done.'
  }
}
finally {
  # Scrub the password from memory and the environment.
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b1)
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b2)
  Remove-Item Env:\OROT_KS_PW -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host '=== Done ===' -ForegroundColor Cyan
Write-Host ''
Write-Host 'BACK UP my-upload-key.jks somewhere off this machine.' -ForegroundColor Yellow
Write-Host 'Lose it and you can never update the app on Play.'
Write-Host ''
Write-Host 'Next: push to main, then download the app-release-aab artifact from'
Write-Host '  https://github.com/yoni333/Orot/actions'
