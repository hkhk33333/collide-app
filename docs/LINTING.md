# Linting Strategy

## Current Setup

**ktlint** (via Trunk): Fast Kotlin formatting (120 chars, explicit imports)
**detekt** (via Gradle): Code quality analysis (complexity, bugs, performance)
**Android Lint** (git hook): Android-specific checks
**Security**: gitleaks + trufflehog for secret scanning
**Documentation**: markdownlint for .md files

**References:** [ktlint](https://ktlint.github.io) | [Detekt Rules](https://detekt.dev/docs/rules/comments)

## Why This Setup?

**Separation of concerns**: ktlint handles formatting, detekt handles analysis

**Version compatibility conflict**:

- Trunk uses ktlint 1.7.1 (latest standards)
- detekt 1.23.8 bundles ktlint 0.50.0 (significantly older)
- These versions have incompatible formatting philosophies that cause
  ping-ponging between tools
- detekt's bundled ktlint cannot be overridden (UberJAR packaging)
- **Solution**: Disable detekt formatting, use ktlint via Trunk for
  consistent modern standards

**Speed**: Fast pre-commit formatting, comprehensive but slower analysis runs separately

**Future**: detekt maintainers committed to adding ktlint 1.x support, timeline TBD

## Compose-Friendly Rules

**Disabled for Android/Compose:**

- `MagicNumber`: UI constants (16.dp, 8.dp) are legitimate
- `LongMethod`/`LongParameterList`: Compose functions are naturally long/parametrized
- `FunctionNaming`: @Composable uses PascalCase (LoginScreen)
- `WildcardImport`: Kotlin style allows wildcards for androidx.compose.*

## Usage

**Quick formatting**: `trunk fmt`
**Linting**: `trunk check`
**Code analysis**: `./gradlew detekt`

**CI**: Fast pre-commit checks (blocking) + comprehensive analysis (monitored)

## Philosophy

**Fast feedback**: Auto-formatting prevents style debates
**Quality insights**: detekt findings guide refactoring
**Team consistency**: Automated standards reduce review friction
