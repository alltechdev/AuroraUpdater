package com.aurora.store.module

import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module to instantiate singleton components for different helpers from gplayapi library
 */
@Module
@InstallIn(SingletonComponent::class)
object HelperModule {

    @Singleton
    @Provides
    fun providesAppDetailsHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): AppDetailsHelper {
        return AppDetailsHelper(authProvider.authData!!)
            .using(httpClient)
    }

    @Singleton
    @Provides
    fun providesPurchaseHelperInstance(
        authProvider: AuthProvider,
        httpClient: IHttpClient
    ): PurchaseHelper {
        return PurchaseHelper(authProvider.authData!!)
            .using(httpClient)
    }
}
