package com.dvt.mobilelogin

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun validation_enablesDisablesLoginButton() {
    val network = InMemoryNetworkMonitor(isOnline = true)
    val tokenStore = InMemoryTokenStore()
    val repo = object : AuthRepository {
      override suspend fun login(email: String, password: String): String = "token"
    }
    val vm = LoginViewModel(repo, network, tokenStore)

    rule.setContent {
      LoginScreen(viewModel = vm)
    }

    rule.onNodeWithTag("loginButton").assertIsNotEnabled()

    rule.onNodeWithTag("email").performTextInput("user@example.com")
    rule.onNodeWithTag("password").performTextInput("123456")

    rule.onNodeWithTag("loginButton").assertIsEnabled()
  }
}
