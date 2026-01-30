# iOS Login (SwiftUI + MVVM)

This folder contains the iOS version of the login assignment implemented as a Swift Package:
- SwiftUI UI (`LoginView`)
- MVVM state management (`LoginViewModel` + `LoginState`)
- Dependency isolation (`AuthServicing`, `NetworkMonitoring`, `TokenStoring`)
- Deterministic async unit tests using XCTest

## Files (as requested)
- `Sources/LoginFeature/LoginViewModel.swift`
- `Sources/LoginFeature/LoginView.swift`
- `Sources/LoginFeature/AuthService.swift`
- `Sources/LoginFeature/NetworkMonitor.swift`
- `Sources/LoginFeature/LoginState.swift`

## Behavior
- Input validation (email non-empty, password >= 6)
- Offline handling (blocks login, shows message)
- Lockout after 3 consecutive failed logins
- Remember Me persists a token (via `TokenStoring`)
- Logout clears token and resets state

### Manual test credentials
- Email: `user@example.com`
- Password: `password`

## Run unit tests
From this folder:

```bash
cd ios-login-swiftui
swift test
```

## Run the included DemoApp (Xcode)
Open `DemoApp/LoginDemoApp.xcodeproj` and run the `LoginDemoApp` scheme on any iOS Simulator.

## Use in an Xcode SwiftUI app (quick way)
1. Create a new iOS App project in Xcode.
2. Add this package via **File → Add Packages…** (choose "Add Local…" and select this folder).
3. In your App’s root view, create the dependencies and render:

- `LoginView(viewModel: LoginViewModel(authService: FakeAuthService(), networkMonitor: StaticNetworkMonitor(isOnline: true), tokenStore: UserDefaultsTokenStore()))`

(For real apps, replace the fakes with real services.)
