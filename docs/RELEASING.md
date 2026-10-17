# Releasing CloakRun

Releases are cut from `main` after each completed phase or feature batch, so the latest build is always on the
[Releases](https://github.com/Arnav-Dugad/CloakRun/releases) page.

## Cutting a release

1. Make sure `main` is green in CI and `CHANGELOG.md` has an `## [Unreleased]` section describing the changes.
2. Run from a clean checkout of `main`:

   ```bash
   scripts/release.sh minor   # or patch / major
   ```

   The script bumps `VERSION_NAME` and `VERSION_CODE` in `version.properties`, moves the Unreleased notes under the
   new version heading, commits `chore(release): vX.Y.Z`, tags `vX.Y.Z` and pushes both.
3. The tag triggers `.github/workflows/release.yml`, which:
   - checks that the tag matches `VERSION_NAME`
   - builds the signed `sideload` release APK and the `play` release AAB
   - verifies the APK is signed with the Arnav Games certificate
   - writes `CloakRun-vX.Y.Z-SHA256SUMS.txt`
   - publishes a GitHub Release with the changelog section, the commit list and the checksums

## Signing

The release keystore lives **outside the repository** at `C:\Users\rduga\CloakRun-Signing\` with its passwords in
`keystore.properties` in the same folder. GitHub Actions gets it from these repository secrets:

| Secret | Contents |
| --- | --- |
| `CLOAKRUN_KEYSTORE_B64` | The `.jks` file, base64-encoded |
| `CLOAKRUN_KEYSTORE_PASSWORD` | Store password |
| `CLOAKRUN_KEY_ALIAS` | `cloakrun` |
| `CLOAKRUN_KEY_PASSWORD` | Key password |
| `GOOGLE_SERVICES_JSON_B64` | `app/google-services.json`, base64-encoded (optional until Phase 3) |

**Back the keystore folder up somewhere safe and offline** (for example an encrypted USB drive and a password
manager). If it is lost, nobody who installed CloakRun can ever receive an update: Android refuses to install an
update signed by a different key.

Release certificate fingerprints (public):

- SHA-1 `90:F8:66:9E:0F:3A:38:C0:97:A5:BA:5D:02:F9:FA:86:70:72:69:4B`
- SHA-256 `41:40:6A:81:F3:0E:5E:10:8E:1A:E5:9D:B6:05:A2:B6:6A:7D:4F:AE:DA:EB:C6:8F:BF:9A:90:CB:CD:FC:AA:72`

## Local signed build

```bash
export CLOAKRUN_KEYSTORE_PATH=C:/Users/rduga/CloakRun-Signing/cloakrun-release.jks
export CLOAKRUN_KEY_ALIAS=cloakrun
# set CLOAKRUN_KEYSTORE_PASSWORD and CLOAKRUN_KEY_PASSWORD from keystore.properties without echoing them
./gradlew assembleSideloadRelease
```

Alternatively copy `keystore.properties` to the repo root (it is git-ignored) and point `storeFile` at the keystore.

## Verifying a download

```bash
sha256sum -c CloakRun-vX.Y.Z-SHA256SUMS.txt
apksigner verify --print-certs CloakRun-vX.Y.Z.apk   # expect the SHA-256 above
```
