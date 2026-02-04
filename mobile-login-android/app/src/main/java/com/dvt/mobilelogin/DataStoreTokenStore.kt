package com.dvt.mobilelogin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Proper async token storage using DataStore.
 * 
 * Unlike SharedPreferences.apply() which starts on the main thread,
 * DataStore is fully async and coroutine-based, ensuring no main thread I/O.
 * 
 * Note: For production, consider using EncryptedDataStore or combining with
 * Android Keystore for encryption at rest.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

class DataStoreTokenStore(private val context: Context) : TokenStore {

  private val tokenKey = stringPreferencesKey("auth_token")

  override suspend fun saveToken(token: String) {
    context.dataStore.edit { prefs ->
      prefs[tokenKey] = token
    }
  }

  override suspend fun clearToken() {
    context.dataStore.edit { prefs ->
      prefs.remove(tokenKey)
    }
  }

  override suspend fun readToken(): String? {
    return context.dataStore.data
      .map { prefs -> prefs[tokenKey] }
      .first()
  }
}
