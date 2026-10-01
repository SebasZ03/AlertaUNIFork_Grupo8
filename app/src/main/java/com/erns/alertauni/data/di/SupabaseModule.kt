package com.erns.alertauni.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://rxgiskmkyockhpnmhxmw.supabase.co",
            supabaseKey = "sb_publishable_iVa3jTMrNSzkFe08ZzKmpQ_Rke4Ae2i"
        ) {
            httpEngine = OkHttp.create()
            install(Functions)
            install(Postgrest)
            install(Auth)
        }
    }
}