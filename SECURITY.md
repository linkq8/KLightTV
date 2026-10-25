# Security policy

KLightTV is a public Android TV / Google TV project. Public visibility does not grant permission to push changes to the protected main branch.

## Report a vulnerability privately

Use [GitHub's private vulnerability reporting](https://github.com/linkq8/KLightTV/security/advisories/new). Include the affected version, steps to reproduce, and expected impact. Do not publish signing keys, ADB keys, passwords, personal device identifiers, or exploit details in public issues.

Reports are reviewed by the maintainer; no guaranteed response time or external security certification is claimed.

## Supported versions and installation

Security fixes are applied to the default branch. Development previews are identified as such in the README; a branch update is not a newly published APK release. Download installers only from this repository's Releases, verify the published SHA-256 checksums, and retain the existing signing identity when updating.

ADB/debugging grants powerful access to your TV. Authorize only your own computer and a trusted local network, disable debugging when finished, and keep backups private. A backup of APKs and launcher settings is not a full Android backup.

## Repository protections

The main branch requires a pull request, up-to-date required CI and resolved review conversations, including for administrators. Force pushes and branch deletion are blocked. Secrets scanning/push protection, Dependabot alerts/security fixes, CodeQL analysis and private reports are enabled in repository settings. Dependency/action update proposals are reviewed before merge; they are not automatically published to devices.

Workflows use read-only default tokens and pinned action commits. Contributions from external users require workflow approval. Security scans reduce risk but cannot guarantee that an application is free of vulnerabilities.

## Network and local-tool boundaries

Settings import/export accepts only external system content providers, rejects file/private schemes and private normalized paths, and bounds the imported JSON size. A third-party document provider is not accepted by this security policy.

Android connections use the platform TLS certificate and hostname validation; cleartext HTTP is disabled. Public weather/calendar data does not carry passwords or account credentials. Certificate pinning is intentionally not used for these third-party public services, to avoid breaking connectivity during certificate rotation; the platform trust store remains in force.

Command-line tools accept local paths and developer-selected executables such as ADB/JAVA_HOME. These are trusted operator inputs, not fields exposed by the TV-manager HTTP API. Commands use argument arrays rather than a host shell. Do not run the tools with untrusted executable paths or environment variables. Legacy diagnostic scripts intentionally read local paths chosen by their operator.
