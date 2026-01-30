import SwiftUI

public struct LoginView: View {
  @StateObject private var viewModel: LoginViewModel

  public init(viewModel: LoginViewModel) {
    _viewModel = StateObject(wrappedValue: viewModel)
  }

  public var body: some View {
    if viewModel.isLoggedIn {
      HomeView(onLogout: {
        Task { await viewModel.logoutTapped() }
      })
    } else {
      form
    }
  }

  private var form: some View {
    VStack(alignment: .leading, spacing: 12) {
      Text("Login").font(.title2).bold()

      TextField("Email", text: Binding(
        get: { viewModel.state.email },
        set: { viewModel.onEmailChanged($0) }
      ))
      #if os(iOS)
      .textInputAutocapitalization(.never)
      .keyboardType(.emailAddress)
      .autocorrectionDisabled(true)
      #endif
      .accessibilityIdentifier("emailField")

      SecureField("Password", text: Binding(
        get: { viewModel.state.password },
        set: { viewModel.onPasswordChanged($0) }
      ))
      .accessibilityIdentifier("passwordField")

      Toggle("Remember me", isOn: Binding(
        get: { viewModel.state.rememberMe },
        set: { viewModel.onRememberMeChanged($0) }
      ))
      .accessibilityIdentifier("rememberMeToggle")

      if let message = viewModel.state.errorMessage {
        Text(message)
          .foregroundStyle(.red)
          .accessibilityIdentifier("errorText")
      }

      Button {
        Task { await viewModel.loginTapped() }
      } label: {
        HStack {
          if viewModel.state.isLoading {
            ProgressView().controlSize(.small)
          }
          Text(viewModel.state.isLoading ? "Logging in…" : "Login")
        }
      }
      .disabled(!viewModel.state.isLoginEnabled)
      .accessibilityIdentifier("loginButton")

      Spacer()
    }
    .padding()
  }
}

private struct HomeView: View {
  let onLogout: () -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: 12) {
      Text("✅ Logged in successfully").font(.title3).bold()
      Text("Welcome!")
      Button("Logout", action: onLogout)
        .accessibilityIdentifier("logoutButton")
      Spacer()
    }
    .padding()
  }
}
