package com.dvt.mobilelogin.di

import android.content.Context
import com.dvt.mobilelogin.AuthRepository
import com.dvt.mobilelogin.ConnectivityNetworkMonitor
import com.dvt.mobilelogin.DataStoreTokenStore
import com.dvt.mobilelogin.FakeAuthRepository
import com.dvt.mobilelogin.NetworkMonitor
import com.dvt.mobilelogin.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing app-wide dependencies.
 * 
 * In production, FakeAuthRepository would be replaced with a real implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideTokenStore(@ApplicationContext context: Context): TokenStore {
    return DataStoreTokenStore(context)
  }

  @Provides
  @Singleton
  fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
    return ConnectivityNetworkMonitor(context)
  }

  @Provides
  fun provideAuthRepository(tokenStore: TokenStore): AuthRepository {
    // TODO: Replace with real AuthRepository implementation for production
    return FakeAuthRepository(tokenStore)
  }
}
