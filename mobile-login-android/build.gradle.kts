// Top-level build file.
// Open this folder in Android Studio; it will sync and fetch Android Gradle Plugin.

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.hilt.android) apply false
  alias(libs.plugins.ksp) apply false
}
