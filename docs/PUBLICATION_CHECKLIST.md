# Repository publication checklist

- [ ] Confirm the MIT copyright holder and permission to license pre-existing DAYFORM source and artwork, including the existing in-app developer credit.
- [ ] Confirm third-party asset/font redistribution requirements.
- [x] Update repository URL in README to `https://github.com/designwithrahma/dayform`.
- [x] Push source and confirm GitHub Actions CI passes (latest verified successful run: 40516261328); source secrets still require a separate maintainer review.
- [ ] Verify the correct release signing key; use the same one for upgrades if applicable.
- [ ] Build the signed APK, install and test on target Android devices.
- [ ] Inspect APK signature and manifest permissions.
- [ ] Configure all four GitHub Actions signing secrets; dispatch release workflow from `main` to create the version tag and **draft** release after verification.
- [ ] Attach signed APK and SHA256SUMS.txt; edit known issues and remove template instructions.
- [ ] Publish GitHub Release after review.

**Current status:** GitHub source and CI build/tests/lint verified; a signed release APK, owner-controlled signing key, device installation/upgrade testing, and publishing remain pending.
