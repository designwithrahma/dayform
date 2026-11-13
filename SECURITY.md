# Security Policy

## Supported releases

DAYFORM v1.0.0 is publicly available as a signed GitHub Release. The maintainer reported testing the APK on an Android phone, but this is not an independent security audit. Security fixes will be prioritized for the latest maintained source and any currently supported public release.

## Reporting a vulnerability

**Do not post exploitable security issues or sensitive evidence in a public issue.** Use GitHub's **Report a vulnerability** private advisory feature in the repository's **Security** tab (enable it in repository settings if necessary). If private reporting is unavailable, request a private contact channel without disclosing exploit details.

Include reproduction steps, affected versions, and potential impact. Please do not test against data or devices you do not own.

## Secrets and release keys

Production signing keys and their passwords must never be committed. GitHub Actions consumes signing material from encrypted repository secrets. If a credential is exposed in existing history, rotate/revoke it as appropriate; deleting the current file alone is insufficient.
