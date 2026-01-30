# DVT Assignment

This workspace contains three independent tasks:

1. **Backend**: Loyalty Points Quote Service (Java + Vert.x)
2. **Android**: Login screen + component tests (Kotlin + Jetpack Compose + MVVM)
3. **iOS**: Login screen + unit tests (SwiftUI + MVVM) + runnable DemoApp (Xcode)

---

## 1) Backend (Vert.x) — `backend/`

### Prerequisites
- JDK 21
- Maven

### Run tests
```bash
cd "backend"
mvn test
```

### Run full verification (includes JaCoCo gate)
```bash
cd "backend"
mvn verify
```

---

## 2) Android (Compose) — `mobile-login-android/`

### Prerequisites
- Android Studio (recommended)
- Android SDK + an emulator/device

### Run from Android Studio
- Open the folder `mobile-login-android` in Android Studio
- Sync Gradle
- Select the `app` configuration and press Run

### Run unit tests from terminal
```bash
cd "mobile-login-android"
./gradlew test
```

### Run instrumented UI tests (requires emulator/device)
```bash
cd "mobile-login-android"
./gradlew connectedAndroidTest
```

---

## 3) iOS (SwiftUI + MVVM) — `ios-login-swiftui/`

### Prerequisites
- Xcode

### Run Swift package unit tests
```bash
cd "ios-login-swiftui"
swift test
```

### Run the included DemoApp (Xcode)
- Open `ios-login-swiftui/DemoApp/LoginDemoApp.xcodeproj`
- Select the `LoginDemoApp` scheme
- Choose any iOS Simulator and press Run

---

## Credentials (for demo login)
- Email: `user@example.com`
- Password: `password`
