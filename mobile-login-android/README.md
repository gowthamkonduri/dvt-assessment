# Mobile Task 2: Android Login Screen (Compose + MVVM)

This folder contains a minimal Android login feature implemented with:
- Kotlin
- Jetpack Compose
- MVVM (`LoginViewModel` + `LoginUiState`)

## What’s implemented
- Validation (enables/disables login button)
- Failure count increments on auth error
- Lockout after 3 failures (blocks further login)
- Offline handling (shows message, no service call)
- Remember me (persists token on success)
- Navigation event on success

## Project note
This is a minimal Gradle Android project skeleton intended to be opened in Android Studio.

## Running tests
- Unit tests (ViewModel logic): run `LoginViewModelTest` from Android Studio.
- UI tests (Compose): run `LoginScreenTest` as an instrumentation test.
