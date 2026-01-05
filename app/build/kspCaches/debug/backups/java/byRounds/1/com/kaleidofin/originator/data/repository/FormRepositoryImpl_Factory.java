package com.kaleidofin.originator.data.repository;

import com.kaleidofin.originator.data.datasource.FormDataSource;
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
public final class FormRepositoryImpl_Factory implements Factory<FormRepositoryImpl> {
  private final Provider<FormDataSource> formDataSourceProvider;

  public FormRepositoryImpl_Factory(Provider<FormDataSource> formDataSourceProvider) {
    this.formDataSourceProvider = formDataSourceProvider;
  }

  @Override
  public FormRepositoryImpl get() {
    return newInstance(formDataSourceProvider.get());
  }

  public static FormRepositoryImpl_Factory create(Provider<FormDataSource> formDataSourceProvider) {
    return new FormRepositoryImpl_Factory(formDataSourceProvider);
  }

  public static FormRepositoryImpl newInstance(FormDataSource formDataSource) {
    return new FormRepositoryImpl(formDataSource);
  }
}
