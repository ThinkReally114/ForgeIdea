package com.forgeidea.di

import com.forgeidea.domain.usecase.SendMessageUseCase
import com.forgeidea.tools.ExecuteCommandTool
import com.forgeidea.ui.chat.ChatViewModel
import com.forgeidea.ui.git.GitViewModel
import com.forgeidea.ui.settings.SettingsViewModel
import com.forgeidea.ui.terminal.TerminalViewModel
import com.forgeidea.ui.theme.ThemeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { SendMessageUseCase(get()) }
    single { ExecuteCommandTool(get()) }
    single { ThemeViewModel(get()) }
    viewModel { ChatViewModel(get(), get(), get(), get()) }
    viewModel { TerminalViewModel(get()) }
    viewModel { GitViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
