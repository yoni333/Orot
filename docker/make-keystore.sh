#!/usr/bin/env bash
# Creates the Play upload keystore inside a throwaway JDK container.
# Nothing is installed on the host - the .jks lands in the repo root via
# the bind mount and the container is discarded.
set -euo pipefail

KEYSTORE="/work/my-upload-key.jks"
B64OUT="/work/keystore.b64.txt"
ALIAS="${KEY_ALIAS:-upload}"

echo
echo "=== Orot - create Play upload keystore ==="
echo

if [ -f "$KEYSTORE" ]; then
  echo "my-upload-key.jks already exists in the repo root."
  echo
  echo "Refusing to overwrite it. Replacing a keystore that has already published"
  echo "an app permanently destroys your ability to update that app."
  echo "Delete it by hand only if you are certain it was never used."
  exit 1
fi

echo "Choose a password for the keystore."
echo "Save it in your password manager NOW - it cannot be recovered."
echo

read -rsp "Password: " PW1; echo
read -rsp "Confirm password: " PW2; echo
echo

[ "$PW1" = "$PW2" ] || { echo "Passwords do not match."; exit 1; }
[ "${#PW1}" -ge 6 ] || { echo "Password must be at least 6 characters (keytool requires it)."; exit 1; }

# -storepass:env keeps the password off the command line, where any other
# process could read it out of the process list.
export OROT_KS_PW="$PW1"

echo "Generating keystore..."
keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -storetype PKCS12 \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias "$ALIAS" \
  -dname "${KEY_DNAME:-CN=Yoni, O=Orot, C=IL}" \
  -storepass:env OROT_KS_PW \
  -keypass:env OROT_KS_PW

keytool -list -keystore "$KEYSTORE" -alias "$ALIAS" -storepass:env OROT_KS_PW >/dev/null
echo "Created and verified my-upload-key.jks"

# base64 -w0 keeps it on a single line so it pastes cleanly into a GitHub secret.
base64 -w0 "$KEYSTORE" > "$B64OUT"
unset OROT_KS_PW

cat <<'NEXT'

=== Done ===

Two files are now in your repo root (both are gitignored):

  my-upload-key.jks   your signing key - BACK THIS UP off this machine.
                      Lose it and you can never update the app on Play.
  keystore.b64.txt    the same key, base64 encoded, for GitHub.

Next: add four repository secrets at
  https://github.com/yoni333/Orot/settings/secrets/actions

  KEYSTORE_BASE64   the entire contents of keystore.b64.txt
  STORE_PASSWORD    the password you just chose
  KEY_PASSWORD      the same password
  KEY_ALIAS         upload

Then delete keystore.b64.txt and push - GitHub Actions builds the signed
.aab for you. Keep my-upload-key.jks in your backup only.
NEXT
