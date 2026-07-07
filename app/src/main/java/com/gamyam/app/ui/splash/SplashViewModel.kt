package com.gamyam.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamyam.app.data.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashState {
    object Loading : SplashState()
    object Success : SplashState()
    data class Error(val message: String) : SplashState()
}

class SplashViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun checkAuth() {
        viewModelScope.launch {
            try {
                authManager.freshToken(forceRefresh = true)
                _state.value = SplashState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = SplashState.Error(e.message ?: "Authentication failed")
            }
        }
    }
}
