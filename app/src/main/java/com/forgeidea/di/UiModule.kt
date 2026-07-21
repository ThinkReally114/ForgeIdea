package com.forgeidea.di

import com.forgeidea.ui.chat.ChatViewModel
import com.forgeidea.ui.settings.SettingsViewModel
import com.forgeidea.ui.theme.ThemeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { ChatViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ThemeViewModel(get()) }
}
