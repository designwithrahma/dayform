# Contributing to DAYFORM

Thanks for helping improve DAYFORM. For a focused, reviewable contribution:

1. Search existing issues before opening a new one.
2. For non-trivial changes, open an issue explaining the expected behavior.
3. Fork the repository, create a descriptive branch, and keep commits scoped.
4. Run `./gradlew :app:assembleDebug :app:lintDebug` and the unit tests in [BUILD.md](docs/BUILD.md).
5. Include a test or a reproducible verification procedure for behavior changes.
6. Open a pull request using the included template; describe screenshots or user-visible differences when applicable.

Do not submit API secrets, keystores, generated APKs, build directories, or private task data. Keep the original `applicationId`, Room database filename, and migration history stable unless an explicit, reviewed migration plan is included. Respect third-party licenses and contributor rights.

Maintainers review contributions as time permits; no response-time promise is made.
