package com.example.iptv.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptv.data.model.LoginCredentials
import com.example.iptv.data.repository.XtreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
        val host: String = "bo.ib3mix.com",
        val port: String = "8080",
        val username: String = "3479184388034045",
        val password: String = "1107619332615286",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val isAuthenticated: Boolean = false,
        val hasTriedAutoLogin: Boolean = false
)

class LoginViewModel(private val repository: XtreamRepository = XtreamRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateHost(host: String) {
        _uiState.value =
                _uiState.value.copy(host = host, errorMessage = null, successMessage = null)
    }

    fun updatePort(port: String) {
        _uiState.value =
                _uiState.value.copy(port = port, errorMessage = null, successMessage = null)
    }

    fun updateUsername(username: String) {
        _uiState.value =
                _uiState.value.copy(username = username, errorMessage = null, successMessage = null)
    }

    fun updatePassword(password: String) {
        _uiState.value =
                _uiState.value.copy(password = password, errorMessage = null, successMessage = null)
    }

    private var lastLoginAttempt = 0L
    private val minDelayBetweenAttempts = 3000L // 3 seconds

    fun login() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAttempt = currentTime - lastLoginAttempt

        if (timeSinceLastAttempt < minDelayBetweenAttempts) {
            val remainingTime = (minDelayBetweenAttempts - timeSinceLastAttempt) / 1000
            _uiState.value =
                    _uiState.value.copy(
                            errorMessage =
                                    "Please wait ${remainingTime} seconds before trying again"
                    )
            return
        }

        lastLoginAttempt = currentTime

        val currentState = _uiState.value
        val credentials =
                LoginCredentials(
                        host = currentState.host.trim(),
                        port = currentState.port.trim(),
                        username = currentState.username.trim(),
                        password = currentState.password.trim()
                )

        if (!credentials.isValid()) {
            _uiState.value = currentState.copy(errorMessage = "Please fill all fields")
            return
        }

        _uiState.value =
                currentState.copy(isLoading = true, errorMessage = null, hasTriedAutoLogin = true)

        viewModelScope.launch {
            try {
                // Add delay to prevent rapid requests
                kotlinx.coroutines.delay(500)

                repository.initialize(credentials)

                repository
                        .authenticate()
                        .onSuccess {
                            _uiState.value =
                                    _uiState.value.copy(
                                            isLoading = false,
                                            successMessage = "Login successful! Connecting...",
                                            errorMessage = null
                                    )
                            // Delay to show success message
                            kotlinx.coroutines.delay(1500)
                            _uiState.value =
                                    _uiState.value.copy(
                                            isAuthenticated = true,
                                            successMessage = null
                                    )
                        }
                        .onFailure { error ->
                            val detailedError = buildString {
                                append("Login Failed\n\n")
                                append("Host: ${credentials.host}:${credentials.port}\n")
                                append("Username: ${credentials.username}\n")
                                append("Error: ${error.message ?: "Unknown error"}\n\n")

                                // Add specific guidance for common errors
                                when {
                                    error.message?.contains("timeout", ignoreCase = true) ==
                                            true -> {
                                        append(
                                                "Tip: Check your internet connection and server address\n"
                                        )
                                    }
                                    error.message?.contains(
                                            "authentication failed",
                                            ignoreCase = true
                                    ) == true -> {
                                        append("Tip: Verify your username and password\n")
                                    }
                                    error.message?.contains("connection", ignoreCase = true) ==
                                            true -> {
                                        append(
                                                "Tip: Check if the server is online and port is correct\n"
                                        )
                                    }
                                }

                                error.cause?.let { cause ->
                                    append("\nRoot cause: ${cause.message}\n")
                                }
                                append("\nNote: Wait at least 3 seconds between attempts")
                            }

                            println("LOGIN ERROR: $detailedError")

                            _uiState.value =
                                    _uiState.value.copy(
                                            isLoading = false,
                                            errorMessage = detailedError,
                                            successMessage = null
                                    )
                        }
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Unexpected error: ${e.message}"
                        )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun getRepository(): XtreamRepository = repository
}
