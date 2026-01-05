package com.kaleidofin.originator.di;

import com.kaleidofin.originator.data.api.FormApiService;
import com.kaleidofin.originator.data.datasource.FormDataSource;
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
public final class AppModule_ProvideFormDataSourceFactory implements Factory<FormDataSource> {
  private final Provider<FormApiService> formApiServiceProvider;

  public AppModule_ProvideFormDataSourceFactory(Provider<FormApiService> formApiServiceProvider) {
    this.formApiServiceProvider = formApiServiceProvider;
  }

  @Override
  public FormDataSource get() {
    return provideFormDataSource(formApiServiceProvider.get());
  }

  public static AppModule_ProvideFormDataSourceFactory create(
      Provider<FormApiService> formApiServiceProvider) {
    return new AppModule_ProvideFormDataSourceFactory(formApiServiceProvider);
  }

  public static FormDataSource provideFormDataSource(FormApiService formApiService) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFormDataSource(formApiService));
  }
}
