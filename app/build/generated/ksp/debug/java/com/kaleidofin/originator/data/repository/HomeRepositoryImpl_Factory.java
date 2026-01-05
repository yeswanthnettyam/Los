package com.kaleidofin.originator.data.repository;

import com.kaleidofin.originator.data.datasource.HomeDataSource;
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
public final class HomeRepositoryImpl_Factory implements Factory<HomeRepositoryImpl> {
  private final Provider<HomeDataSource> homeDataSourceProvider;

  public HomeRepositoryImpl_Factory(Provider<HomeDataSource> homeDataSourceProvider) {
    this.homeDataSourceProvider = homeDataSourceProvider;
  }

  @Override
  public HomeRepositoryImpl get() {
    return newInstance(homeDataSourceProvider.get());
  }

  public static HomeRepositoryImpl_Factory create(Provider<HomeDataSource> homeDataSourceProvider) {
    return new HomeRepositoryImpl_Factory(homeDataSourceProvider);
  }

  public static HomeRepositoryImpl newInstance(HomeDataSource homeDataSource) {
    return new HomeRepositoryImpl(homeDataSource);
  }
}
