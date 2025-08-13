# Linting & Formatting Guide

## Current Approach

### ktlint (via Trunk) - Formatting

- **Purpose**: All code formatting using standard Kotlin conventions
- **When it runs**: On file save, pre-commit hooks, `.trunk/tools/trunk fmt`
- **What it checks**: Kotlin formatting (120 chars, explicit imports, indentation)
- **Speed**: Very fast (~seconds)

### detekt (via Gradle) - Analysis Only

- **Purpose**: Code quality analysis (formatting disabled)
- **When it runs**: During builds, CI pipeline, manual runs
- **What it checks**:
  - Code complexity and maintainability
  - Potential bugs and code smells
  - Performance issues
  - Naming conventions
- **Speed**: Slower (~10-30 seconds) but comprehensive

## Why This Setup?

**Originally**, we wanted to use detekt's formatting (which wraps ktlint) for a unified toolchain. However, we encountered a version conflict:

- Trunk uses ktlint 1.7.1 (latest)
- Detekt 1.23.8 bundles ktlint 0.50.0 (older)

These versions have **incompatible indentation rules** that caused ping-ponging formatting conflicts. After 11+ months waiting for detekt to update to ktlint 1.x, we disabled detekt formatting to resolve the conflict.

**Benefits of current approach**:

- No formatting conflicts
- Uses latest ktlint standards
- Clear separation of concerns
- Still get comprehensive code quality analysis from detekt

## Configuration Decisions

### Disabled Rules & Reasoning

We've disabled certain detekt rules that don't align with modern Android/Compose development:

#### **MagicNumber** (disabled)

```yaml
MagicNumber:
  active: false # Android UI uses many numeric constants
```

**Why**: Android UI code has many legitimate constants (16.dp, 8.dp, animation durations, etc.)

#### **WildcardImport** (disabled)

```yaml
WildcardImport:
  active: false # Kotlin style guide allows wildcards for common packages
```

**Why**: Kotlin style guide permits wildcards for common packages like `kotlinx.*` and `androidx.compose.*`

#### **LongMethod** (disabled)

```yaml
LongMethod:
  active: false # Compose UI functions can be naturally long
```

**Why**: Compose UI functions are declarative and can legitimately be long without being complex

#### **LongParameterList** (disabled)

```yaml
LongParameterList:
  active: false # Compose functions often need many parameters
```

**Why**: Compose functions often require many parameters for customization and state management

#### **FunctionNaming** (disabled)

```yaml
FunctionNaming:
  active: false # Conflicts with @Composable PascalCase convention
```

**Why**: `@Composable` functions use PascalCase (e.g., `LoginScreen()`) which conflicts with camelCase rule

#### **MatchingDeclarationName** (disabled)

```yaml
MatchingDeclarationName:
  active: false # Android allows naming flexibility
```

**Why**: Android conventions allow flexibility (e.g., `MainActivity` class in `MainActivity.kt`)

### Standard Kotlin Conventions

We follow ktlint's defaults:

- **120 character lines**: Standard Kotlin convention
- **Explicit imports**: Clear dependency visibility (no wildcards)
- **4-space indentation**: Standard for Kotlin code

## Commands & Usage

### Quick Development Workflow

```bash
# Format files (fast)
.trunk/tools/trunk fmt

# Quick lint check (fast)
.trunk/tools/trunk check

# Check specific files
.trunk/tools/trunk check app/src/main/java/com/test/testing/MainActivity.kt
```

### Code Quality Analysis

```bash
# Full detekt analysis (no formatting)
./gradlew detekt

# Generate detekt reports
./gradlew detekt
# Reports: app/build/reports/detekt/
```

### CI/CD Integration

Our CI pipeline runs:

1. **Fast checks** (ktlint via Trunk) - blocking
2. **Comprehensive analysis** (detekt) - non-blocking but monitored

## Troubleshooting

### Common Issues

#### "Formatting conflict between ktlint and detekt"

- **Cause**: Version mismatch (trunk ktlint 1.7.1 vs detekt ktlint 0.50.0)
- **Solution**: Disabled detekt formatting, use only ktlint via Trunk

#### "detekt takes too long"

- **Cause**: Running detekt on every small change
- **Solution**: Use `.trunk/tools/trunk fmt` for quick fixes, detekt for thorough checks

#### "IDE formatting differs from ktlint"

- **Cause**: IDE settings not matching ktlint defaults
- **Solution**: Configure your IDE to use 120-char lines and explicit imports, or just rely on ktlint via Trunk to format on save

### Getting Help

1. Check this documentation first
2. Run `.trunk/tools/trunk --help` for Trunk commands
3. Run `./gradlew detekt --help` for detekt options
4. For rule-specific questions, see [detekt documentation](https://detekt.dev)

## Best Practices

### For Developers

- Run `.trunk/tools/trunk fmt` before committing
- Fix any ktlint issues immediately (they're quick)
- Review detekt reports weekly for code quality insights

### For Code Reviews

- ktlint violations should be auto-fixed, not discussed
- Focus code review discussions on detekt findings
- Use detekt reports to identify refactoring opportunities

### For CI/CD

- ktlint violations block merges (fast feedback)
- detekt issues are informational but should be addressed
- Track detekt metrics over time for code quality trends
