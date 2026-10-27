# Creating a DAYFORM GitHub Release

This document describes the release procedure; it is **not** evidence that a signed APK has already been produced.

## 1. Preconditions

- Confirm you hold the rights to publish the source, in-app artwork and third-party assets under the declared licenses. In-app developer attribution and the existing package ID are intentionally retained.
- Ensure `./gradlew :app:assembleDebug`, the non-screenshot unit tests, and lint pass on a configured Android toolchain.
- Test normal installation, onboarding, task CRUD, reminders, calendar, export/import, orientation and process recovery on actual Android devices or emulators.
- Use a secure release keystore with a recorded alias and durable backup. If updating users who installed an earlier APK, verify the new APK is signed with the **same** required key.
- Confirm `versionName` (`1.0.0`), `versionCode` (`1`) and release tag (`v1.0.0`) agree. Increase `versionCode` for future app updates.

## 2. Create a signed APK

Follow [BUILD.md](BUILD.md). Do **not** commit the keystore or passwords. After setting all four `DAYFORM_*` environment variables, build with:

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

1. Upload the extracted source **contents**, not the outer ZIP, to the new GitHub repository. Check the `gradlew` executable bit (`git update-index --chmod=+x gradlew` if necessary).
2. Confirm `.github/workflows/android-ci.yml` completes successfully and no secrets appear in Git history.
3. Set up protected GitHub Actions repository secrets if you want the optional release workflow: `DAYFORM_KEYSTORE_B64` (base64 of the JKS), `DAYFORM_STORE_PASSWORD`, `DAYFORM_KEY_ALIAS`, `DAYFORM_KEY_PASSWORD`.
4. Commit and push the source, then tag that exact commit `v1.0.0` (`git tag v1.0.0 && git push origin v1.0.0`).
5. Either manually create a **draft** release for that tag and upload your signed `DAYFORM-v1.0.0.apk` and `SHA256SUMS.txt`, or dispatch the `Signed draft release` workflow against that tag after configuring secrets.
6. Paste the appropriate text from `docs/RELEASE_NOTES_TEMPLATE.md`. Verify installed behavior and checksum, then publish the draft.

The included release workflow intentionally creates a **draft**; publishing requires a human verification step.

## 4. Security / compatibility

A public source archive does not contain an APK by design. Do not upload an unverified release or claim CI passed if it has not run. Do not change `applicationId`, signing identity, or Room migrations without a deliberate upgrade plan. GitHub may limit the ability to upload an executable APK from certain interfaces; use the Release assets upload area.
