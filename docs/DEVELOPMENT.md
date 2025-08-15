# Development Guide

## Local Tooling

- Trunk CLI manages linters, formatters, and hooks
  - Install/upgrade: `./.trunk/tools/trunk upgrade -y`
  - Format: `./.trunk/tools/trunk fmt`
  - Check: `./.trunk/tools/trunk check`
  - See [LINTING.md](LINTING.md) for comprehensive linting guide

- Commitizen (Gitmoji) for commits
  - Run `gt create` or `gt modify` and follow the prompts
  - Emoji-first format is validated by `commitlint.config.js`

## Running Locally

- Build debug: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Lint: `./gradlew :app:lintDebug`
- Detekt: `./gradlew :app:detekt` (code quality analysis, non-blocking in CI)
- Coverage (unit tests): `./gradlew :app:koverXmlReport :app:koverHtmlReport`
- Instrumentation tests: `./gradlew :app:connectedDebugAndroidTest` (needs emulator/device)

## CI Expectations

- PRs must pass: unit tests, Android Lint, and emulator smoke (API 30)
- Post-merge to main also runs Detekt and the full emulator matrix (API 29/30/33)
- Nightly at 02:00 UTC re-runs matrix only if the main HEAD changed since the last nightly

## Troubleshooting

- Formatter/CI disagreement (e.g., YAML quoting): prefer unquoted cron values; comments explain schedules
- Slow emulator boot in CI: ensure AVD cache keys match API/arch/target
- Trunk hook issues: `./.trunk/tools/trunk git-hooks sync`
