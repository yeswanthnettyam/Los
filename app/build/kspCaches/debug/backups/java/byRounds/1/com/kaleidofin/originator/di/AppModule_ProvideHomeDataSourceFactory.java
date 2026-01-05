package com.kaleidofin.originator.di;

import com.kaleidofin.originator.data.datasource.HomeDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideHomeDataSourceFactory implements Factory<HomeDataSource> {
  @Override
  public HomeDataSource get() {
    return provideHomeDataSource();
  }

  public static AppModule_ProvideHomeDataSourceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HomeDataSource provideHomeDataSource() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHomeDataSource());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideHomeDataSourceFactory INSTANCE = new AppModule_ProvideHomeDataSourceFactory();
  }
}
