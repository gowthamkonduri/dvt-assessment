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

      // Email field with validation feedback
      TextField("Email", text: Binding(
        get: { viewModel.state.email },
        set: { viewModel.onEmailChanged($0) }
      ))
      .textFieldStyle(RoundedBorderTextFieldStyle())
      .overlay(
        RoundedRectangle(cornerRadius: 5)
          .stroke(emailBorderColor, lineWidth: 1)
      )
      #if os(iOS)
      .textInputAutocapitalization(.never)
      .keyboardType(.emailAddress)
      .autocorrectionDisabled(true)
      #endif
      .accessibilityLabel("Email address")
      .accessibilityHint("Enter your email address")
      .accessibilityIdentifier("emailField")

      // Password field with validation feedback
      SecureField("Password", text: Binding(
        get: { viewModel.state.password },
        set: { viewModel.onPasswordChanged($0) }
      ))
      .textFieldStyle(RoundedBorderTextFieldStyle())
      .overlay(
        RoundedRectangle(cornerRadius: 5)
          .stroke(passwordBorderColor, lineWidth: 1)
      )
      .accessibilityLabel("Password")
      .accessibilityHint("Enter your password. Must be at least 8 characters with uppercase, lowercase, and number.")
      .accessibilityIdentifier("passwordField")

      Toggle("Remember me", isOn: Binding(
        get: { viewModel.state.rememberMe },
        set: { viewModel.onRememberMeChanged($0) }
      ))
      .accessibilityIdentifier("rememberMeToggle")

      if let message = viewModel.state.errorMessage {
        Text(message)
          .foregroundStyle(.red)
          .accessibilityLabel("Error: \(message)")
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
      .accessibilityLabel(viewModel.state.isLoading ? "Logging in" : "Login button")
      .accessibilityHint(viewModel.state.isLoginEnabled ? "Double tap to log in" : "Enter valid email and password to enable")
      .accessibilityIdentifier("loginButton")

      Spacer()
    }
    .padding()
    .task {
      await viewModel.checkStoredToken()
    }
  }

  // MARK: - Validation Colors

  private var emailBorderColor: Color {
    if viewModel.state.email.isEmpty { return .gray }
    return viewModel.state.isValidEmail ? .green : .red
  }

  private var passwordBorderColor: Color {
    if viewModel.state.password.isEmpty { return .gray }
    return viewModel.state.isValidPassword ? .green : .red
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
