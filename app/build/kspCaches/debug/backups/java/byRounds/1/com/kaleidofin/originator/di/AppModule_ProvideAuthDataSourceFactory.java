package com.kaleidofin.originator.di;

import com.kaleidofin.originator.data.datasource.AuthDataSource;
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
public final class AppModule_ProvideAuthDataSourceFactory implements Factory<AuthDataSource> {
  @Override
  public AuthDataSource get() {
    return provideAuthDataSource();
  }

  public static AppModule_ProvideAuthDataSourceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AuthDataSource provideAuthDataSource() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthDataSource());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideAuthDataSourceFactory INSTANCE = new AppModule_ProvideAuthDataSourceFactory();
  }
}
