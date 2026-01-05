package com.kaleidofin.originator.di;

import com.kaleidofin.originator.data.datasource.HomeDataSource;
import com.kaleidofin.originator.domain.repository.HomeRepository;
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
public final class AppModule_ProvideHomeRepositoryFactory implements Factory<HomeRepository> {
  private final Provider<HomeDataSource> homeDataSourceProvider;

  public AppModule_ProvideHomeRepositoryFactory(Provider<HomeDataSource> homeDataSourceProvider) {
    this.homeDataSourceProvider = homeDataSourceProvider;
  }

  @Override
  public HomeRepository get() {
    return provideHomeRepository(homeDataSourceProvider.get());
  }

  public static AppModule_ProvideHomeRepositoryFactory create(
      Provider<HomeDataSource> homeDataSourceProvider) {
    return new AppModule_ProvideHomeRepositoryFactory(homeDataSourceProvider);
  }

  public static HomeRepository provideHomeRepository(HomeDataSource homeDataSource) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHomeRepository(homeDataSource));
  }
}
