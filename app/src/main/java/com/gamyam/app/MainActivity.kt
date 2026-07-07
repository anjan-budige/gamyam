package com.gamyam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.gamyam.app.data.AppSettingsRepository
import com.gamyam.app.data.AppThemeMode
import com.gamyam.app.theme.GamyamTheme

import com.gamyam.app.data.AuthManager
import com.gamyam.app.ui.splash.SplashViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val authManager = AuthManager(applicationContext)
    val settingsRepository = AppSettingsRepository(applicationContext)

    enableEdgeToEdge()
    setContent {
      val themeMode by settingsRepository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
      val systemDark = isSystemInDarkTheme()
      val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }
      GamyamTheme(darkTheme = darkTheme) { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
            MainNavigation(authManager = authManager) 
        } 
      }
    }
  }
}

// Simple Factory for ViewModel
class SplashViewModelFactory(private val authManager: AuthManager) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
