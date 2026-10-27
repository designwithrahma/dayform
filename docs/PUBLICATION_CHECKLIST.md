# Repository publication checklist

- [ ] Confirm the MIT copyright holder and permission to license pre-existing DAYFORM source and artwork, including the existing in-app developer credit.
- [ ] Confirm third-party asset/font redistribution requirements.
- [x] Update repository URL in README to `https://github.com/designwithrahma/dayform`.
- [ ] Push source and confirm GitHub Actions CI passes; no credentials committed.
- [ ] Verify the correct release signing key; use the same one for upgrades if applicable.
- [ ] Build the signed APK, install and test on target Android devices.
- [ ] Inspect APK signature and manifest permissions.
- [ ] Tag v1.0.0 only for the exact reviewed commit; then create draft release.
- [ ] Attach signed APK and SHA256SUMS.txt; edit known issues and remove template instructions.
- [ ] Publish GitHub Release after review.

**Current status:** source prepared; signing, SDK-backed compilation, device testing, and GitHub CI execution remain unverified in this offline environment.
