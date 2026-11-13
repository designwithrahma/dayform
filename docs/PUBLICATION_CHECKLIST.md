# Repository publication checklist

**Status: DAYFORM v1.0.0 was published on 2026-11-13.**

- [ ] Independently confirm ownership and MIT licensing rights for all original DAYFORM source, app artwork, and preserved in-app attribution.
- [ ] Complete an owner-level review of third-party asset and font distribution requirements.
- [x] Publish source and updated README at https://github.com/designwithrahma/dayform.
- [x] GitHub Actions Android CI passed on the README-update commit before publishing.
- [x] Build a signed APK in the signed-release workflow; verify the APK signature and include SHA-256 digest.
- [x] Attach `DAYFORM-v1.0.0.apk` and `SHA256SUMS.txt` to the release.
- [x] Maintainer reported testing the APK on an Android phone; detailed test results and device information have not been documented.
- [x] Replace draft release notes with public notes and publish v1.0.0.
- [ ] Document full hands-on test matrix, known issues, and signing-key continuity for future updates.

**Published release:** https://github.com/designwithrahma/dayform/releases/tag/v1.0.0

The successful release workflow establishes that the APK was signed and verified by the build tools. It does not substitute for a full independent security audit or comprehensive physical-device QA. Keep the signing key private and securely backed up.
