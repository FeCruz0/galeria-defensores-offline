package com.galeria.defensores.di

import android.content.Context
import com.galeria.defensores.data.database.AppDatabase
import com.galeria.defensores.data.database.daos.*
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideCharacterDao(database: AppDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    fun provideTableDao(database: AppDatabase): TableDao {
        return database.tableDao()
    }

    @Provides
    fun provideRuleSystemDao(database: AppDatabase): RuleSystemDao {
        return database.ruleSystemDao()
    }
}
