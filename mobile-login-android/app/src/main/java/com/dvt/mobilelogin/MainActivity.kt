package com.dvt.mobilelogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          // In a real app you'd inject implementations. For this assignment we keep it minimal.
          val networkMonitor = remember { InMemoryNetworkMonitor(isOnline = true) }
          val context = LocalContext.current
          val tokenStore = remember { SharedPreferencesTokenStore(context.applicationContext) }
          val authRepository = remember { FakeAuthRepository(tokenStore) }

          val vm = remember {
            LoginViewModel(
              authRepository = authRepository,
              networkMonitor = networkMonitor,
              tokenStore = tokenStore
            )
          }

          val scope = rememberCoroutineScope()

          var isLoggedIn by remember { mutableStateOf(false) }

          LaunchedEffect(tokenStore) {
            isLoggedIn = tokenStore.readToken() != null
          }

          LaunchedEffect(vm) {
            vm.events.collect { event ->
              if (event is LoginEvent.NavigateToHome) {
                isLoggedIn = true
              }
            }
          }

          if (isLoggedIn) {
            HomeScreen(
              onLogout = {
                scope.launch {
                  tokenStore.clearToken()
                  vm.resetForLogout()
                  isLoggedIn = false
                }
              }
            )
          } else {
            LoginScreen(viewModel = vm)
          }
        }
      }
    }
  }
}
