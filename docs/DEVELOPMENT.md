# Development Guide

## Getting Started

### Initial Setup

1. **Clone and open the project**:

   ```bash
   git clone https://github.com/hkhk33333/locshare.git
   cd locshare
   ```

2. **Setup Trunk CLI**:

   ```bash
   # Install Trunk CLI globally
   curl https://get.trunk.io -fsSL | bash

   # Setup trunk for this project (downloads tools and installs git hooks)
   trunk install
   ```

   **Alternative for VS Code users**:
   - Install the Trunk extension in VS Code
   - Extension will detect `.trunk/trunk.yaml` and help with linting/formatting in the editor

3. **Verify setup**:
   ```bash
   ./.trunk/tools/trunk --version
   ./.trunk/tools/trunk check --sample  # Test linters
   ```

### Git Hooks Setup

Trunk automatically manages git hooks for:

- **Pre-commit**: Formatting (trunk-fmt), linting (trunk-check), secret scanning (trufflehog)
- **Commit-msg**: Emoji format validation (commitlint), commitizen prompts
- **Pre-push**: Additional checks (trunk-check-pre-push), npm security checks

If hooks get out of sync, run:

```bash
./.trunk/tools/trunk git-hooks sync
```

## Local Tooling

- Trunk CLI manages linters, formatters, and hooks
  - Install/upgrade: `./.trunk/tools/trunk upgrade -y`
  - Format: `./.trunk/tools/trunk fmt`
  - Check: `./.trunk/tools/trunk check`
  - See [LINTING.md](LINTING.md) for comprehensive linting guide

- Commitizen (Gitmoji) for commits
  - Run `gt create` or `gt modify` and follow the prompts
  - Emoji-first format is validated by `commitlint.config.js`

## Commit Message Format

Two formats are accepted (both require emoji):

**Option A - Emoji-first** (current default):

- Format: `emoji (scope): description`
- Example: `🐛 (auth): fix login validation`
- Example: `✨ add user profile feature`

**Option B - Conventional with emoji**:

- Format: `type(scope): emoji description`
- Example: `fix(auth): 🐛 resolve login issue`
- Example: `feat: ✨ add user profile feature`
- Note: Requires config change to enable conventional mode

For emoji reference, see [gitmoji.dev](https://gitmoji.dev)

**How to commit**:

- Use `gt create`/`gt modify` for interactive emoji selection
- Direct `git commit` messages must match one of the formats above

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
