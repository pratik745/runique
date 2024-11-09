package com.pratik.auth.presentation.login

import androidx.compose.foundation.text2.input.TextFieldState

data class LoginState(
    val email: TextFieldState = TextFieldState(),
    val password: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    val isLoggingIn: Boolean = false,
    val canLogin: Boolean = false
)
