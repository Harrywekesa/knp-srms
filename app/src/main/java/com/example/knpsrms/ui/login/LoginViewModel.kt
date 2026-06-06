package com.example.knpsrms.ui.login

import androidx.lifecycle.ViewModel
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(private val repository: DataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(username: String, password: String, role: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Username and password cannot be empty")
            return
        }
        
        _uiState.value = LoginUiState.Loading
        try {
            val user = repository.login(username.trim(), password, role)
            if (user != null) {
                _uiState.value = LoginUiState.Success(user)
            } else {
                _uiState.value = LoginUiState.Error("Invalid credentials or role selected")
            }
        } catch (e: Exception) {
            _uiState.value = LoginUiState.Error("Database error: ${e.message}")
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
