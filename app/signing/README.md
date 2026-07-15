# WafuStudyShield Sideload Signing Key

`wafustudyshield-sideload.p12` is a repo-local signing key used only to make GitHub Release APKs installable from Android package installers when private Actions signing secrets are not available.

This key is public because it is committed to the repository. It is suitable for transparent sideload builds from this repository, not for Play Store or private production distribution. If private signing secrets are configured in GitHub Actions, the release workflow uses those secrets instead.

Public key details:

- Alias: `wafustudyshield-sideload`
- Store password: `wafustudyshield`
- Key password: `wafustudyshield`
