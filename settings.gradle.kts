pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Java Toolchain Auto-Provisioning
    // Enables automatic download of required Java versions when not locally available
    // Ensures consistent builds across different developer environments
    // Downloads to Gradle User Home for reuse across projects
    // Added: August 2025 (Gradle 9.0 upgrade)
    // Ref: https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "testing"
include(":app")
