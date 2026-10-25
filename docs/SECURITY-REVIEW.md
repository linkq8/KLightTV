# Repository security review — 25 October 2026

This review concerns the public repository and its source. It is not a certification of released APKs, a penetration test of physical televisions, or a full-device backup.

## Repository controls verified

- Repository remains public.
- `main` requires an up-to-date `checks` result from GitHub Actions, a pull request and resolved conversations. Administrators are included; force pushes and deletion are blocked. No independent reviewer is claimed: required approval count is zero for this single-maintainer project.
- Secret scanning and push protection enabled; Dependabot alerts and security updates enabled; private vulnerability reporting enabled.
- CodeQL extended analysis configured for Java/Kotlin, JavaScript/TypeScript, Python and Actions, with remote and local threat models. Initial analysis succeeded in all four groups.
- External contributors require approval to run workflows. Workflow tokens default to read-only; action versions are pinned to official commit SHAs, with weekly Dependabot update proposals.

## Secret and alert review

Gitleaks 8.30.1, verified against the official release checksum, scanned all 248 reachable commits at the beginning of this review (about 26 MB). No leaks were detected. GitHub secret-scanning and dependency-alert queries also returned no open findings at that time. This does not prove that every possible secret format is detectable.

Initial CodeQL analysis produced 36 findings. They were reviewed by source location, rather than treating scanner severity alone as proof of exploitability:

- Actual hardening changes: remove signing credentials from console output, create credential files exclusively with mode 0600, restrict returned document URIs to external system providers and reject private/file schemes and normalized private paths, apply signature permission to all operations on the photo provider, cap bitmap sample growth, replace a potentially quadratic calendar-label regex with a forward scan, bound/simplify version and package validation, use exact hostnames in the browser test, pin Actions and set token permissions.
- Local operator-input findings: ADB/JAVA_HOME/PATH executable selection, explicitly selected calendar/package-list files, and local upload/snapshot paths are intended capabilities of local CLI tools. They are not supplied by the authenticated loopback manager API. Arguments use arrays, not a host shell. These were triaged as false positives or test-only usage, with per-alert rationale. Do not pass untrusted executables or environment variables to these tools.
- Certificate pinning findings: public weather requests use standard platform HTTPS certificate and hostname validation, with no custom trust bypass and no account credentials. Pinning third-party service certificates is deliberately not added. Explicit cleartext prohibition was added to both Android apps. The pinning recommendations are documented accepted design decisions, not dismissed evidence of a TLS bypass.

A post-merge scan also recommended making the already constrained release-version path explicitly recognizable as an allowlist. Version validation now uses three bounded ASCII numeric components; tests verify that path traversal, URLs, query strings, Unicode digits, newlines and oversized versions cause no network request.

Scanner dismissals are visible in GitHub with rationale. Findings corrected by code should close after the patched default branch is reanalyzed; scan status must be checked separately from configuration status.

## Validation

Both Android debug APKs compile and Android lint passes. Existing lint issues involving numeric UI constants were replaced with named constants without changing behavior. The launcher intentionally uses the HOME category rather than a separate Leanback app entry; its legacy optional remote log permission still requires an explicit operator grant and is not granted by this patch.

The full existing test suite passed, including native prayer calculations, date rollover, reminders, manager/installer checks and browser offline/rendering behavior. New signing-helper regression tests use dummy key bytes and verify no credential output, private file permissions, no replacement of an existing key and rejection of a dangling symlink. No real signing key was generated or replaced during this review.

No physical TV was connected or modified, and no APK release was published. Sharing the public repository link is different from recommending an unaudited installer or arbitrary debloating commands. Use the published installation safety policy and keep ADB access restricted to trusted computers.
