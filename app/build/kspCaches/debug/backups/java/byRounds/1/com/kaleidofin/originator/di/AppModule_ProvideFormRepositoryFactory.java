package com.kaleidofin.originator.di;

import com.kaleidofin.originator.data.datasource.FormDataSource;
import com.kaleidofin.originator.domain.repository.FormRepository;
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
public final class AppModule_ProvideFormRepositoryFactory implements Factory<FormRepository> {
  private final Provider<FormDataSource> formDataSourceProvider;

  public AppModule_ProvideFormRepositoryFactory(Provider<FormDataSource> formDataSourceProvider) {
    this.formDataSourceProvider = formDataSourceProvider;
  }

  @Override
  public FormRepository get() {
    return provideFormRepository(formDataSourceProvider.get());
  }

  public static AppModule_ProvideFormRepositoryFactory create(
      Provider<FormDataSource> formDataSourceProvider) {
    return new AppModule_ProvideFormRepositoryFactory(formDataSourceProvider);
  }

  public static FormRepository provideFormRepository(FormDataSource formDataSource) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFormRepository(formDataSource));
  }
}
