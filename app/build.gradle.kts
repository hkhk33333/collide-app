/**
 * Java Configuration
 * ==================
 * Toolchain: Java 21 LTS | Build JVM: Varies (17+ required by Gradle 9) | Target: Java 21
 * Configured: August 2025 (Gradle 9.0 upgrade)
 *
 * Definitions:
 * - Toolchain: JVM used to compile code (Java 21, auto-downloaded if missing)
 * - Build JVM: JVM running Gradle itself (min Java 17, not project-controlled)
 * - Target: Bytecode compatibility (Java 21, could be lower if needed)
 *
 * Decision rationale:
 * - Java 21 target: Current LTS (supported until 2029)
 * - Not Java 22: Non-LTS with only 6 months support
 * - Toolchain ensures consistent compilation across all developer machines
 * - Detekt 1.23.8 constraint: Supports JVM targets up to 22
 * - No desugaring needed: Project uses no Java 8+ APIs (java.time, streams, NIO)
 *
 * Note: While Gradle 9 requires Java 17+ to run, the target bytecode could be
 * as low as Java 8 if needed for older Android devices. We use 21 for modern features.
 *
 * Desugaring: Disabled - enable if using java.time/streams/NIO:
 * compileOptions.isCoreLibraryDesugaringEnabled = true
 * dependencies { coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4") }
 *
 * Upgrade path: Java 25 LTS (Sept 2025) when tooling supports it
 *
 * References:
 * - https://developer.android.com/build/jdks
 * - https://docs.gradle.org/current/userguide/toolchains.html
 * - https://detekt.dev/docs/introduction/compatibility/
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.control.plugin)
    id("kotlin-parcelize")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

android {
    namespace = "com.test.testing"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.test.testing"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Default build config for production/standard builds
        buildConfigField("boolean", "USE_DISCORD_SYSTEM", "false")
        buildConfigField("String", "DISCORD_BACKEND_URL", "\"https://miniworld-new-staging.up.railway.app\"")
        buildConfigField("String", "DISCORD_CLIENT_ID", "\"1232840493696680038\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "USE_DISCORD_SYSTEM", "false")
        }
        debug {
            // Firebase system for standard debug builds
            buildConfigField("boolean", "USE_DISCORD_SYSTEM", "true") // Enable for debug
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        xmlReport = false
        htmlReport = true
        sarifReport = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Firebase (for co-existing system and FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Lifecycle and ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Networking (for Discord System)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Image Loading (for Discord System)
    implementation(libs.coil.compose)

    // Auth (for Discord System)
    implementation(libs.androidx.browser)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    detektPlugins(libs.detekt.formatting)
}

kover {
    reports {
        filters {
            excludes {
                classes("*.BuildConfig", "*.R", "*.R$*", "androidx.*")
            }
        }
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = false
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("${project.rootDir}/detekt.yml"))
    ignoreFailures = true
    autoCorrect = true
}

// Ensure CI artifact uploads have reports available
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
        sarif.required.set(true) // enables trunk to catch detekt issues when running trunk check
        txt.required.set(false)
    }
}

// Secrets plugin configuration for injecting API keys from local.properties
// For CI/CD, uses local.defaults.properties when local.properties is not present
secrets {
    // Use local.properties for actual secrets (not in version control)
    propertiesFileName = "local.properties"
    // Fallback for CI/CD builds with safe placeholder values
    defaultPropertiesFileName = "local.defaults.properties"
    // Ignore SDK directory which is already handled by Gradle
    ignoreList.add("sdk.*")
}
