# Creating a DAYFORM GitHub Release

This document describes the release procedure; it is **not** evidence that a signed APK has already been produced.

## 1. Preconditions

- Confirm you hold the rights to publish the source, in-app artwork and third-party assets under the declared licenses. In-app developer attribution and the existing package ID are intentionally retained.
- Ensure `./gradlew :app:assembleDebug`, the non-screenshot unit tests, and lint pass on a configured Android toolchain.
- Test normal installation, onboarding, task CRUD, reminders, calendar, export/import, orientation and process recovery on actual Android devices or emulators.
- Use a secure release keystore with a recorded alias and durable backup. If updating users who installed an earlier APK, verify the new APK is signed with the **same** required key.
- Confirm `versionName` (`1.0.0`), `versionCode` (`1`) and release tag (`v1.0.0`) agree. Increase `versionCode` for future app updates.

## 2. Create a signed APK

Follow [BUILD.md](BUILD.md) and the [Windows signing and GitHub Secrets guide](SIGNING_SETUP_WINDOWS.md). Do **not** commit the keystore or passwords. After setting all four `DAYFORM_*` environment variables, build with:

```bash
./gradlew :app:assembleRelease
```

Install and test the signed output. On a machine with Android SDK build-tools, validate the signature:

```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Produce a clearly named asset and SHA-256 digest:

```bash
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk dist/DAYFORM-v1.0.0.apk
cd dist
sha256sum DAYFORM-v1.0.0.apk > SHA256SUMS.txt
```

**Do not** rename a debug APK as a release APK. If a release cannot be built, the public release remains blocked.

## 3. GitHub delivery

1. The public source repository is already pushed to `main`, and Android CI has passed. Verify the latest `main` CI status again after release-workflow edits.
2. On your own computer, create or reuse a long-term signing key and privately back it up. Follow [SIGNING_SETUP_WINDOWS.md](SIGNING_SETUP_WINDOWS.md).
3. Add the four protected repository secrets: `DAYFORM_KEYSTORE_B64`, `DAYFORM_STORE_PASSWORD`, `DAYFORM_KEY_ALIAS`, `DAYFORM_KEY_PASSWORD`.
4. Open [Signed draft release](https://github.com/designwithrahma/dayform/actions/workflows/release.yml), choose **Run workflow**, use branch `main` and input `v1.0.0`.
5. The workflow validates the version, checks for an existing tag/release, runs tests and lint, builds and verifies the signed release APK, computes its SHA-256 checksum, and creates a **draft** with both assets. The tag is created automatically at the exact workflow commit only if these steps succeed. **Do not manually create the tag first.**
6. As maintainer, download the draft APK and test it on Android devices, including backup/import and upgrade signing continuity where applicable. Check the attached `SHA256SUMS.txt`.
7. Edit the draft notes to record real tested devices and known issues, remove the release-note template's editor instructions, then publish the draft.

**The workflow never publishes the draft for you.** Do not publish before signature/device testing and source/asset licensing checks.

## 4. Security / compatibility

A public source archive does not contain an APK by design. Do not upload an unverified release or claim CI passed if it has not run. Do not change `applicationId`, signing identity, or Room migrations without a deliberate upgrade plan. GitHub may limit the ability to upload an executable APK from certain interfaces; use the Release assets upload area.
