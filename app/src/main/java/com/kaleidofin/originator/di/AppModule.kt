package com.kaleidofin.originator.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kaleidofin.originator.data.api.FormApiService
import com.kaleidofin.originator.data.api.FormApiServiceDummy
import com.kaleidofin.originator.data.datasource.AuthDataSource
import com.kaleidofin.originator.data.datasource.FormDataSource
import com.kaleidofin.originator.data.datasource.HomeDataSource
import com.kaleidofin.originator.data.datasource.impl.AuthDataSourceImpl
import com.kaleidofin.originator.data.datasource.impl.FormDataSourceImpl
import com.kaleidofin.originator.data.datasource.impl.HomeDataSourceImpl
import com.kaleidofin.originator.data.repository.AuthRepositoryImpl
import com.kaleidofin.originator.data.repository.FormRepositoryImpl
import com.kaleidofin.originator.data.repository.HomeRepositoryImpl
import com.kaleidofin.originator.domain.repository.AuthRepository
import com.kaleidofin.originator.domain.repository.FormRepository
import com.kaleidofin.originator.domain.repository.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideAuthDataSource(): AuthDataSource {
        return AuthDataSourceImpl()
    }

    @Provides
    @Singleton
    fun provideHomeDataSource(): HomeDataSource {
        return HomeDataSourceImpl()
    }

    @Provides
    @Singleton
    fun provideFormApiService(gson: Gson): FormApiService {
        return FormApiServiceDummy(gson)
    }

    @Provides
    @Singleton
    fun provideFormDataSource(formApiService: FormApiService): FormDataSource {
        return FormDataSourceImpl(formApiService)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authDataSource: AuthDataSource): AuthRepository {
        return AuthRepositoryImpl(authDataSource)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(homeDataSource: HomeDataSource): HomeRepository {
        return HomeRepositoryImpl(homeDataSource)
    }

    @Provides
    @Singleton
    fun provideFormRepository(formDataSource: FormDataSource): FormRepository {
        return FormRepositoryImpl(formDataSource)
    }
}

