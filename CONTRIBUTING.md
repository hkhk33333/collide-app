# Contributing

This project uses Trunk to manage code quality (linters, formatters, security checks) and Git hooks. This guide explains our setup, why specific hooks are enabled, how CI runs, and how to work around Commitizen hook issues.

## Trunk overview

- Install/upgrade everything: `./.trunk/tools/trunk upgrade -y`
- Run all checks locally: `./.trunk/tools/trunk check`
- Format code: `./.trunk/tools/trunk fmt`
- See enabled actions/hooks: `./.trunk/tools/trunk actions list`

Configuration lives in `.trunk/trunk.yaml`. Trunk plugins are checked in under `.trunk/plugins/` and tools are shimmed under `.trunk/tools/`.
Commit message configs live at the repo root:

- `commitlint.config.js` – validates messages and supports Gitmoji
- `.czrc` – Commitizen adapter (`cz-emoji`)

## Git hooks managed by Trunk

These hooks run automatically on their respective Git events. We enable only what is useful for this repo and fast enough for local workflows.

Enabled actions (hooks) and why:

- `trunk-fmt-pre-commit` (pre-commit): Runs `trunk fmt` to auto-format changes. Keeps diffs clean.
- `trunk-check-pre-commit` (pre-commit): Runs `trunk check` on staged changes. Catches issues early.
- `trunk-check-pre-push` (pre-push): Runs `trunk check` before pushing. Safety net for missed local checks.
- `commitlint` (commit-msg): Validates commit messages. Configured to accept Gitmoji formats and optional Conventional Commits.
- `trufflehog-pre-commit` (pre-commit): Scans for secrets in staged files.
- `git-blame-ignore-revs` (post-checkout): Keeps blame useful by ignoring specified bulk-change commits.
- `submodule-init-update` (post-merge/checkout): Keeps submodules up to date if used.
- `npm-check-pre-push` (pre-push): Notifies if Node deps are stale (harmless for non-Node repos).
- `trunk-announce`, `trunk-upgrade-available` (background): Quality-of-life notices for Trunk.

Commitizen:

- `commitizen` (prepare-commit-msg): Enabled with the `cz-emoji` adapter for an emoji-first prompt.

You can temporarily bypass hooks with standard Git flags: `git commit --no-verify` and `git push --no-verify` (avoid in normal workflows).

## Commit messages (Gitmoji + Commitizen)

Commitlint validates commit messages on the `commit-msg` hook. We support both Gitmoji styles:

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

Gitmoji reference: [gitmoji.dev](https://gitmoji.dev)

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

## Using `gt modify`

- Use `gt modify` and follow the Commitizen prompts.
- Or pass a message explicitly. Both formats are accepted by commitlint:
  - Emoji-first: `gt modify -m ":rotating_light: (project) setup trunk"`
  - Conventional + emoji: `gt modify -m "lint(project) :rotating_light: setup trunk"`

## CI: GitHub Actions

We run Trunk in CI using official actions. See `.github/workflows/`:

- `trunk.yml`: Runs on push and pull_request, posts annotations on failures.
- `trunk-upgrade.yml`: Nightly job that opens a PR to upgrade Trunk linters/tools.

No extra secrets are required; the default `GITHUB_TOKEN` is sufficient for annotations and upgrade PRs.

## Upgrading Trunk, linters, and runtimes

- Upgrade everything to latest validated versions:
  - `./.trunk/tools/trunk upgrade -y check plugins runtimes tools cli`
- Versions are pinned in `.trunk/trunk.yaml` after upgrades. Runtimes are kept to stable major versions per plugin channel.

## Troubleshooting

- Commitizen breaks `git commit --amend` or tooling that re-invokes `git commit`:
  - Ensure `commitizen` action is disabled in `.trunk/trunk.yaml`.
  - Use manual composer (`./.trunk/tools/commitizen` or `npx commitizen`).
- Hooks not running:
  - Re-sync: `./.trunk/tools/trunk git-hooks sync`
  - Verify hooks path: `git config core.hooksPath` should point to Trunk’s cache directory.
- Slow checks:
  - Use `./.trunk/tools/trunk check --filter <path or linter>` locally to scope runs.

## FAQ

- Why not enable every possible hook? We enable what adds value without disrupting common flows. Hooks that block amends or are redundant for this repo are disabled.
- Can I run Trunk on all files locally? Yes: `./.trunk/tools/trunk check --all`.
