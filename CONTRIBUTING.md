# Contributing

This project uses Trunk for code quality (linting, formatting, security)
and git hooks. This guide covers setup, development workflow, and commit standards.

## Development Setup

1. **Initial setup**: `trunk install` (installs tools and git hooks)
2. **Format code**: `trunk fmt`
3. **Check code**: `trunk check`
4. **Upgrade tools**: `trunk upgrade`

**Note**: Use `trunk` if installed globally, or `./.trunk/tools/trunk` for local
installation. VS Code users can use the Trunk extension for editor integration.

Configuration: `.trunk/trunk.yaml` | Commit validation: `commitlint.config.js`

**Resources:** [Trunk Docs](https://docs.trunk.io) | [Trunk VS Code Extension](https://marketplace.visualstudio.com/items?itemName=trunk.io)

## Android Development

**Build & Test:**

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Coverage: `./gradlew koverHtmlReport`
- Instrumentation tests: `./gradlew connectedDebugAndroidTest` (requires device/emulator)

**Code Quality:**

- Android Lint: `./gradlew lintDebug`
- Detekt analysis: `./gradlew detekt` ([Detekt Rules](https://detekt.dev/docs/rules/comments))

## Git hooks managed by Trunk

Current git hooks (automatically managed by Trunk):

- `trunk-fmt-pre-commit` (pre-commit): Auto-formats code before commits
- `trufflehog-pre-commit` (pre-commit): Scans for secrets in staged files
- `trunk-check-pre-commit` (pre-commit): Runs linters on staged changes
- `android-lint-pre-commit` (pre-commit): Android-specific lint checks
- `trunk-check-pre-push` (pre-push): Additional linting before push
- `gradle-test-pre-push` (pre-push): Runs unit tests before push
- `gradle-build-pre-push` (pre-push): Verifies project builds before push
- `commitlint` (commit-msg): Validates Gitmoji commit message format
- `git-blame-ignore-revs` (post-checkout): Keeps git blame useful
- `trunk-announce`, `trunk-upgrade-available` (background): Trunk notifications

Commitizen:

- `commitizen` (prepare-commit-msg): Enabled with the `cz-emoji` adapter for an
  emoji-first prompt.

You can temporarily bypass hooks with standard Git flags: `git commit --no-verify`
and `git push --no-verify` (avoid in normal workflows).

## Commit messages (Gitmoji + Commitizen)

Commitlint validates commit messages on the `commit-msg` hook.
We support both Gitmoji styles:

- Gitmoji default (emoji-first): `:emoji: (scope)?: subject`
  - Example: `:rotating_light: (project) setup trunk`
- Gitmoji + Conventional (type first): `type(scope)? :emoji: subject`
  - Example: `lint(project) :rotating_light: setup trunk`

Toggle conventional style by adding to `package.json`:

```json
{
  "config": { "cz-emoji": { "conventional": true } }
}
```

**Resources:** [Gitmoji Reference](https://gitmoji.dev) | [Commitlint Rules](https://commitlint.js.org/)

## Commitizen configuration

- `.czrc` uses the `cz-emoji` adapter:

  ```json
  { "path": "cz-emoji" }
  ```

- Trunk installs the adapter for the commit hook via an action override:

  ```yaml
  actions:
    enabled:
      - commitizen
      - commitlint
      # ...
    definitions:
      - id: commitizen
        packages_file: ${workspace}/.trunk/commitizen/package.json
  ```

- Adapter dependencies are declared in `/.trunk/commitizen/package.json`:

  ```json
  {
    "private": true,
    "dependencies": { "commitizen": "^4.3.0", "cz-emoji": "^1.3.1" }
  }
  ```

## Graphite Workflow (Stacked PRs)

We use **Graphite** for stacked PR development:
[Graphite Docs](https://graphite.dev/docs) | [Stacked PRs Guide](https://graphite.dev/docs/stacking)

**Basic workflow:**

- `gt create` - Create new branch and commit
- `gt modify` - Amend current commit (with Gitmoji prompts)
- `gt submit` - Create/update PR for current stack
- `gt sync` - Sync with remote and restack

**Stacking:**

- `gt branch -c feature-2` - Create branch stacked on current
- `gt up`/`gt down` - Navigate between stack levels
- `gt restack` - Rebase entire stack after changes

**Commit messages** (both accepted):

- Emoji-first: `gt modify -m ":bug: fix login validation"`
- Conventional: `gt modify -m "fix: :bug: resolve login issue"`

## CI Pipeline

**PR/Push:** Unit tests, Android Lint, instrumentation tests (API 30), Detekt analysis
**Nightly:** Full emulator matrix (API 29/30/33) if main branch changed
**Auto-upgrades:** Trunk tools upgraded nightly via PR

## Dependency Updates

**Dependabot:** Weekly Gradle + GitHub Actions updates (Mondays 09:00 UTC)
**Trunk:** Nightly linter/tool upgrades via automated PR

## Troubleshooting

**Hooks not running:** `trunk git-hooks sync`
**Slow checks:** `trunk check --filter <path>` to scope runs
**Bypass hooks:** `git commit --no-verify` (avoid in normal workflow)
