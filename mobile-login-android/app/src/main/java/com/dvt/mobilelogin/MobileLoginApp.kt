package com.dvt.mobilelogin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class required for Hilt dependency injection.
 * Must be registered in AndroidManifest.xml.
 */
@HiltAndroidApp
class MobileLoginApp : Application()
