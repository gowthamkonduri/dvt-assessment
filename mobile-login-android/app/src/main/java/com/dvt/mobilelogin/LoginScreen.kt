package com.dvt.mobilelogin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
  val state by viewModel.uiState.collectAsState()

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
        .testTag("email"),
      value = state.email,
      onValueChange = viewModel::onEmailChanged,
      label = { Text("Email") },
      singleLine = true,
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("password"),
      value = state.password,
      onValueChange = viewModel::onPasswordChanged,
      label = { Text("Password") },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
      Text(text = if (state.isOnline) "Online" else "Offline", modifier = Modifier.testTag("networkStatus"))

      Spacer(modifier = Modifier.height(4.dp))

      Column {
        Checkbox(
          checked = state.rememberMe,
          onCheckedChange = viewModel::onRememberMeChanged,
          modifier = Modifier.testTag("rememberMe")
        )
        Text(text = "Remember me")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (state.errorMessage != null) {
      Text(
        text = state.errorMessage!!,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("error")
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    Button(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("loginButton"),
      onClick = viewModel::onLoginClicked,
      enabled = state.isLoginEnabled,
    ) {
      Text(text = if (state.isLoading) "Logging in…" else "Login")
    }
  }
}
