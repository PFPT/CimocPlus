package com.haleydu.cimoc.di

import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun preferenceManager(): PreferenceManager
    fun sourceManager(): SourceManager
}
