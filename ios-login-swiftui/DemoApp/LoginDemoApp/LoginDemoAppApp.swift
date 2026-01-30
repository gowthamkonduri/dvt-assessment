import SwiftUI
import LoginFeature

@main
struct LoginDemoAppApp: App {
  var body: some Scene {
    WindowGroup {
      LoginView(
        viewModel: LoginViewModel(
          authService: FakeAuthService(),
          networkMonitor: StaticNetworkMonitor(isOnline: true),
          tokenStore: UserDefaultsTokenStore()
        )
      )
    }
  }
}
