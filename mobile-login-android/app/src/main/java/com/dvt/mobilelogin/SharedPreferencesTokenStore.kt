package com.dvt.mobilelogin

import android.content.Context

class SharedPreferencesTokenStore(
  context: Context,
) : TokenStore {

  private val prefs = context.getSharedPreferences("mobilelogin", Context.MODE_PRIVATE)

  override suspend fun saveToken(token: String) {
    prefs.edit().putString(KEY_TOKEN, token).apply()
  }

  override suspend fun clearToken() {
    prefs.edit().remove(KEY_TOKEN).apply()
  }

  override suspend fun readToken(): String? = prefs.getString(KEY_TOKEN, null)

  private companion object {
    private const val KEY_TOKEN = "token"
  }
}
