package com.kaleidofin.originator.di;

import com.kaleidofin.originator.data.datasource.AuthDataSource;
import com.kaleidofin.originator.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<AuthDataSource> authDataSourceProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<AuthDataSource> authDataSourceProvider) {
    this.authDataSourceProvider = authDataSourceProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(authDataSourceProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(
      Provider<AuthDataSource> authDataSourceProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(authDataSourceProvider);
  }

  public static AuthRepository provideAuthRepository(AuthDataSource authDataSource) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(authDataSource));
  }
}
