package com.dvt.mobilelogin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure token storage using EncryptedSharedPreferences.
 * Tokens are encrypted at rest using AES256-GCM.
 */
class EncryptedTokenStore(context: Context) : TokenStore {

  private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

  private val prefs = EncryptedSharedPreferences.create(
    context,
    PREFS_FILE_NAME,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
  )

  override suspend fun saveToken(token: String) {
    prefs.edit().putString(KEY_TOKEN, token).apply()
  }

  override suspend fun clearToken() {
    prefs.edit().remove(KEY_TOKEN).apply()
  }

  override suspend fun readToken(): String? = prefs.getString(KEY_TOKEN, null)

  private companion object {
    private const val PREFS_FILE_NAME = "secure_prefs"
    private const val KEY_TOKEN = "auth_token"
  }
}
