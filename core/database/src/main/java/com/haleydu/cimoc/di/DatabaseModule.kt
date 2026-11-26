package com.haleydu.cimoc.di

import android.content.Context
import androidx.room.Room
import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.db.CimocMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CimocDatabase =
        Room.databaseBuilder(context.applicationContext, CimocDatabase::class.java, "cimoc.db")
            .addMigrations(*CimocMigrations.ALL)
            .build()

    @Provides
    fun provideComicDao(database: CimocDatabase) = database.comicDao()

    @Provides
    fun provideChapterDao(database: CimocDatabase) = database.chapterDao()

    @Provides
    fun provideTagDao(database: CimocDatabase) = database.tagDao()

    @Provides
    fun provideTagRefDao(database: CimocDatabase) = database.tagRefDao()

    @Provides
    fun provideSourceDao(database: CimocDatabase) = database.sourceDao()

    @Provides
    fun provideSourceRuleDao(database: CimocDatabase) = database.sourceRuleDao()

    @Provides
    fun provideSourceHealthDao(database: CimocDatabase) = database.sourceHealthDao()

    @Provides
    fun provideTaskDao(database: CimocDatabase) = database.taskDao()

    @Provides
    fun provideImageUrlDao(database: CimocDatabase) = database.imageUrlDao()
}
