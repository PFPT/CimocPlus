package com.haleydu.cimoc.di

import android.content.Context
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.network.NetworkPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CimocModule {

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager =
        PreferenceManager(context)

    @Provides
    @Singleton
    fun provideNetworkPolicy(preferenceManager: PreferenceManager): NetworkPolicy =
        NetworkPolicy {
            preferenceManager.getBoolean(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false)
        }
}
