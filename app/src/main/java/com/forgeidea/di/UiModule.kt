package com.forgeidea.di

import com.forgeidea.ui.chat.ChatViewModel
import com.forgeidea.ui.settings.SettingsViewModel
import com.forgeidea.ui.theme.ThemeState
import com.forgeidea.ui.theme.ThemeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { ThemeState(get<com.forgeidea.data.datastore.ApiKeyStore>().getTheme()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ThemeViewModel(get(), get()) }
}
