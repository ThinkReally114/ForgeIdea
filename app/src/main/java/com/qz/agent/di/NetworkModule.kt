package com.qz.agent.di

import com.qz.agent.data.datastore.ApiKeyStore
import com.qz.agent.llm.LlmClient
import com.qz.agent.llm.OpenAiCompatibleClient
import com.qz.agent.llm.ZenConfig
import com.qz.agent.llm.stream.SseParser
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
        val keyStore = get<ApiKeyStore>()
        val apiKey = keyStore.getZenApiKey() ?: ""
        OpenAiCompatibleClient(
            httpClient = get(),
            config = ZenConfig(apiKey = apiKey)
        )
    }
}
