package com.pratik.auth.presentation.di

import com.pratik.auth.presentation.login.LoginViewModel
import com.pratik.auth.presentation.register.RegisterViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val authViewModelModule = module {
    viewModelOf(::RegisterViewModel)
    viewModelOf(::LoginViewModel)
}