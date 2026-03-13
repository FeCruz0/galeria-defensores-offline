package com.galeria.defensores.di

import com.galeria.defensores.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCharacterRepository(characterDao: com.galeria.defensores.data.database.daos.CharacterDao): CharacterRepository {
        return CharacterRepository(characterDao)
    }

    @Provides
    @Singleton
    fun provideTableRepository(
        tableDao: com.galeria.defensores.data.database.daos.TableDao,
        characterRepository: CharacterRepository
    ): TableRepository {
        return TableRepository(tableDao, characterRepository)
    }

    @Provides
    @Singleton
    fun provideRuleSystemRepository(ruleSystemDao: com.galeria.defensores.data.database.daos.RuleSystemDao): RuleSystemRepository {
        return RuleSystemRepository(ruleSystemDao)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        characterRepository: CharacterRepository,
        tableRepository: TableRepository
    ): BackupRepository {
        return BackupRepository(characterRepository, tableRepository)
    }
}
