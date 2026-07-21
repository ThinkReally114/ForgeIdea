package com.forgeidea.di

import com.forgeidea.domain.usecase.SendMessageUseCase
import com.forgeidea.ui.chat.ChatViewModel
import com.forgeidea.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { SendMessageUseCase(get()) }
    viewModel { ChatViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
