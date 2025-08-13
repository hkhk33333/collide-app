# Android CI and Quality Gates

This document explains our GitHub Actions CI, what runs on each event, and how to reproduce locally.

## Overview

We use a single workflow at `.github/workflows/android-ci.yml` with distinct jobs:

- Build and Unit Tests (with Kover coverage) — path-gated
- Android Lint — path-gated
- Detekt — path-gated; runs on PR and push
- Instrumentation Tests (PR smoke, API 30, with AVD cache) — path-gated
- Instrumentation Tests Matrix (nightly only: API 29/30/33) + manual dispatch
- Nightly Change Guard (skips nightly matrix runs if the main HEAD hasn’t changed since the previous run)

Java 17 is used (AGP 8.x requirement). Gradle caching is handled by `gradle/actions/setup-gradle@v4`.

## Events and Triggers

- pull_request (any branch):
  - Path-gated Build + Unit Tests (Kover html/xml reports uploaded)
  - Path-gated Android Lint (HTML report uploaded)
  - Path-gated Instrumentation Tests (API 30 smoke, AVD cache)
  - Path-gated Detekt
- push (to `main` only):
  - Same as PR (path-gated)
- schedule (Nightly at 02:00 UTC):
  - Nightly Change Guard checks last recorded sha; matrix runs only if HEAD changed
  - Instrumentation Tests Matrix (API 29/30/33)
- workflow_dispatch:
  - Manually trigger full matrix as needed
- schedule (Nightly at 02:00 UTC):
  - Nightly Change Guard checks last recorded sha; matrix runs only if HEAD changed

## Cost and Speed Optimizations

- Single-API emulator smoke (PRs) keeps checks fast and budget-friendly
- Full emulator matrix runs only on push/nightly
- Detekt is push-only and path-gated by `dorny/paths-filter` (skips docs/markdown-only changes)
- AVD cache (API-specific keys) speeds up emulator start
- Gradle wrapper validation is provided by `setup-gradle@v4`

## Artifacts

- Unit test reports: `app/build/test-results/testDebugUnitTest/**`, `app/build/reports/tests/testDebugUnitTest/**`
- Coverage: `app/build/reports/kover/html/**`, `app/build/reports/kover/xml/**`
- Lint: `app/build/reports/lint-results-debug.html`
- Instrumentation tests: `app/build/outputs/androidTest-results/**`, `app/build/reports/androidTests/connected/**`

## Local Reproduction

- Unit tests: `./gradlew :app:testDebugUnitTest`
- Build app: `./gradlew :app:assembleDebug`
- Lint: `./gradlew :app:lintDebug`
- Detekt (non-blocking in CI): `./gradlew :app:detekt`
- Coverage (unit tests): `./gradlew :app:koverXmlReport :app:koverHtmlReport`
- Instrumentation tests (requires device/emulator): `./gradlew :app:connectedDebugAndroidTest`

## Nightly Change Guard Details

- Restores cached `.github/.nightly_cache/last_sha` via `actions/cache/restore@v4`
- Compares current `GITHUB_SHA` to `last_sha`
- If unchanged, matrix job is skipped
- If changed, updates `last_sha` and saves cache via `actions/cache/save@v4`

## Future Enhancements (Optional)

- Codecov upload (Kover XML) once org approval exists; add `codecov.yml` with informational thresholds
- Coverage thresholds for PR gating (unit only or combined)
- Additional Detekt rules once the baseline stabilizes
