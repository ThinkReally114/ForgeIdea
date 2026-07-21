package com.forgeidea.di

import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.llm.LlmClient
import com.forgeidea.llm.OpenAiCompatibleClient
import com.forgeidea.llm.stream.SseParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) { level = LogLevel.NONE }
        }
    }
    single { SseParser() }
    single { ApiKeyStore(get()) }
    single<LlmClient> {
        OpenAiCompatibleClient(
            httpClient = get(),
            keyStore = get(),
            sseParser = get()
        )
    }
}
