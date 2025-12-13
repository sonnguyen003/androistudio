// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            // Force consistent kotlin-stdlib version
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
        }
        // Exclude old kotlin-stdlib-jdk7/jdk8 that conflict with kotlin-stdlib 1.8+
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}