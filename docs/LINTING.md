# Linting Strategy

## Current Setup

**ktlint** (via Trunk): Fast Kotlin formatting (120 chars, explicit imports)
**detekt** (via Gradle, invoked by Trunk): Code quality analysis
(complexity, bugs, performance)
**Android Lint** (git hook): Android-specific checks
**Security**: gitleaks + trufflehog for secret scanning
**Documentation**: markdownlint for .md files

Important: detekt runs through Gradle (`detekt-gradle`).
That path requires a system JDK (Java 17+). Reason:
Gradle 9 requires Java 17+ to run. Trunk’s optional Java runtime is not used by `detekt-gradle`.
If `JAVA_HOME` is missing in clean environments (e.g., containers),
detekt will fail until a JDK is installed.
See: [Gradle 9 Java compatibility](https://docs.gradle.org/9.0/userguide/compatibility.html#java)

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
- `WildcardImport`: Kotlin style allows wildcards for androidx.compose.\*

## Usage

Prerequisite for detekt (local or CI):
JDK 17+ available on PATH with `JAVA_HOME` set.

- Quick formatting: `trunk fmt`
- Linting (runs ktlint, gitleaks, etc., and invokes detekt via Gradle): `trunk check`
- Direct detekt run: `./gradlew detekt`

CI: GitHub Actions uses `actions/setup-java@v4` (Temurin 21) before Gradle steps.
Replicate locally in a container by installing OpenJDK 17+
before running `trunk check`.

## Philosophy

**Fast feedback**: Auto-formatting prevents style debates
**Quality insights**: detekt findings guide refactoring
**Team consistency**: Automated standards reduce review friction
