package com.dvt.mobilelogin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
  val state by viewModel.uiState.collectAsState()
  val focusManager = LocalFocusManager.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .padding(16.dp)
  ) {
    Text(text = "Login", style = MaterialTheme.typography.headlineSmall)

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("email")
        .semantics { contentDescription = "Email address input field" },
      value = state.email,
      onValueChange = viewModel::onEmailChanged,
      label = { Text("Email") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
      ),
      keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) }
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("password")
        .semantics { contentDescription = "Password input field" },
      value = state.password,
      onValueChange = viewModel::onPasswordChanged,
      label = { Text("Password") },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done
      ),
      keyboardActions = KeyboardActions(
        onDone = {
          focusManager.clearFocus()
          if (state.isLoginEnabled) viewModel.onLoginClicked()
        }
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = if (state.isOnline) "Online" else "Offline",
        modifier = Modifier
          .testTag("networkStatus")
          .semantics {
            contentDescription = if (state.isOnline) "Network status: Online" else "Network status: Offline"
          }
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Checkbox(
          checked = state.rememberMe,
          onCheckedChange = viewModel::onRememberMeChanged,
          modifier = Modifier
            .testTag("rememberMe")
            .semantics {
              contentDescription = if (state.rememberMe) "Remember me, checked" else "Remember me, unchecked"
            }
        )
        Text(
          text = "Remember me",
          modifier = Modifier.clickable { viewModel.onRememberMeChanged(!state.rememberMe) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (state.errorMessage != null) {
      Text(
        text = state.errorMessage!!,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
          .testTag("error")
          .semantics { contentDescription = "Error: ${state.errorMessage}" }
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    Button(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("loginButton")
        .semantics {
          contentDescription = if (state.isLoading) {
            "Logging in, please wait"
          } else if (state.isLoginEnabled) {
            "Login button, double tap to sign in"
          } else {
            "Login button, disabled. Enter valid email and password to enable"
          }
        },
      onClick = viewModel::onLoginClicked,
      enabled = state.isLoginEnabled,
    ) {
      Text(text = if (state.isLoading) "Logging in…" else "Login")
    }
  }
}
