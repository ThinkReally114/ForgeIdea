package com.forgeidea.di

import com.forgeidea.domain.usecase.SendMessageUseCase
import com.forgeidea.tools.ExecuteCommandTool
import com.forgeidea.ui.chat.ChatViewModel
import com.forgeidea.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { SendMessageUseCase(get()) }
    single { ExecuteCommandTool(get()) }
    viewModel { ChatViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
