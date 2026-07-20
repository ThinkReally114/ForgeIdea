package com.qz.agent.di

import com.qz.agent.domain.usecase.SendMessageUseCase
import com.qz.agent.ui.chat.ChatViewModel
import com.qz.agent.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { SendMessageUseCase(get()) }
    viewModel { ChatViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
