package com.dvt.mobilelogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          val vm: LoginViewModel = hiltViewModel()
          val isLoggedIn by vm.isLoggedIn.collectAsState()

          if (isLoggedIn) {
            HomeScreen(onLogout = vm::logout)
          } else {
            LoginScreen(viewModel = vm)
          }
        }
      }
    }
  }
}
