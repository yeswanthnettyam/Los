package com.kaleidofin.originator.data.repository;

import com.kaleidofin.originator.data.datasource.AuthDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthDataSource> authDataSourceProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthDataSource> authDataSourceProvider) {
    this.authDataSourceProvider = authDataSourceProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(authDataSourceProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthDataSource> authDataSourceProvider) {
    return new AuthRepositoryImpl_Factory(authDataSourceProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthDataSource authDataSource) {
    return new AuthRepositoryImpl(authDataSource);
  }
}
