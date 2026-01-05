package com.kaleidofin.originator.data.datasource.impl;

import com.kaleidofin.originator.data.api.FormApiService;
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
public final class FormDataSourceImpl_Factory implements Factory<FormDataSourceImpl> {
  private final Provider<FormApiService> formApiServiceProvider;

  public FormDataSourceImpl_Factory(Provider<FormApiService> formApiServiceProvider) {
    this.formApiServiceProvider = formApiServiceProvider;
  }

  @Override
  public FormDataSourceImpl get() {
    return newInstance(formApiServiceProvider.get());
  }

  public static FormDataSourceImpl_Factory create(Provider<FormApiService> formApiServiceProvider) {
    return new FormDataSourceImpl_Factory(formApiServiceProvider);
  }

  public static FormDataSourceImpl newInstance(FormApiService formApiService) {
    return new FormDataSourceImpl(formApiService);
  }
}
