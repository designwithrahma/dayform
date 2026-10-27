# DAYFORM release signing — Windows

This guide creates or reuses the private signing key needed for GitHub's **Signed draft release** workflow. The workflow publishes a **draft only** after building and verifying the signed APK; it does not make the app public automatically.

## 1. Protect your app's signing identity

- **If DAYFORM already has a non-debug release signing key, reuse that key.** Android updates to an installed app generally require the same signing certificate.
- If you only installed a **debug** APK, a new release-signed APK will normally **not** install over it. Export your task workspace from the app first, then uninstall the debug version before installing the signed release. Uninstalling may delete local data.
- The keystore and its passwords belong to the maintainer. Keep two secure offline backups, preferably in separate protected locations.
- **Never** commit the `.jks` file, send the private key through chat, add it to an issue, or share signing passwords.
- Review source/artwork ownership and existing credits before publishing any release.

## 2. Create a key, only if this is the first non-debug release

On your **own Windows PC**, install JDK 21 (Android Studio or another trusted JDK installer). Open PowerShell and run:

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\dayform-release.jks" -storetype PKCS12 -alias dayform -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=DAYFORM, OU=Android, O=DAYFORM, C=LK"
```

Type a strong password when asked; do not put it on the command line. For PKCS12, the private-key password is usually the same as the keystore password. Record the **keystore path**, **password**, and **alias** (`dayform`) privately. If a key already exists, do **not** run this command or replace it.

To check the key locally (this command prompts for its password):

```powershell
keytool -list -v -keystore "$env:USERPROFILE\dayform-release.jks" -alias dayform
```

## 3. Add GitHub Actions repository secrets

Open [GitHub Actions secrets](https://github.com/designwithrahma/dayform/settings/secrets/actions) and add **four repository secrets**:

| Secret name | Value |
| --- | --- |
| `DAYFORM_KEYSTORE_B64` | Base64-encoded bytes of your actual `.jks` file |
| `DAYFORM_STORE_PASSWORD` | Your keystore password |
| `DAYFORM_KEY_ALIAS` | `dayform` (or the alias of your existing signing key) |
| `DAYFORM_KEY_PASSWORD` | Your private-key password |

To place the base64 keystore in your Windows clipboard without printing it on screen:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\dayform-release.jks")) | Set-Clipboard
```

Immediately paste it into the `DAYFORM_KEYSTORE_B64` GitHub secret. Do not paste it into the repository or a chat. Clear your clipboard afterward. If reusing a different key location, edit the path in this PowerShell command.

## 4. Build a v1.0.0 draft release

Once all secrets are saved:

1. Open the [Signed draft release workflow](https://github.com/designwithrahma/dayform/actions/workflows/release.yml).
2. Select **Run workflow**.
3. Select branch `main` and confirm input `v1.0.0`.
4. Run the workflow. It checks the Gradle version, runs tests and lint, builds a signed APK, verifies its signature, and creates a draft.
5. The draft receives `DAYFORM-v1.0.0.apk` and `SHA256SUMS.txt`; the `v1.0.0` Git tag is created automatically **only after successful validation**. Do not manually create a tag first.

The source repository is public, but the release remains **draft** until you publish it. If any step fails, inspect the job log; do not publish a debug or unsigned APK as a substitute.

## 5. Verify and publish

- Download the **signed** APK from the draft release as the maintainer.
- Verify the certificate/signature and checksum, and test installation on an actual Android device.
- Test onboarding, tasks/subtasks, notifications, reminders, calendar, export/import, rotation, and relaunch.
- Test upgrading from a prior **release-signed** version if one exists, using the same key.
- On Windows, compare the downloaded APK hash against `SHA256SUMS.txt`: `Get-FileHash -Algorithm SHA256 .\DAYFORM-v1.0.0.apk`.
- Replace the review instructions in the draft notes with real device test results and known issues, then publish.

**Do not mark the release ready before the on-device verification and ownership checks are complete.**
