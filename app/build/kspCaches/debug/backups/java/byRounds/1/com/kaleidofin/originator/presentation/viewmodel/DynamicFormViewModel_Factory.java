package com.kaleidofin.originator.presentation.viewmodel;

import com.kaleidofin.originator.data.datasource.FormDataSource;
import com.kaleidofin.originator.domain.usecase.GetFormConfigurationUseCase;
import com.kaleidofin.originator.domain.usecase.GetMasterDataUseCase;
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
public final class DynamicFormViewModel_Factory implements Factory<DynamicFormViewModel> {
  private final Provider<GetFormConfigurationUseCase> getFormConfigurationUseCaseProvider;

  private final Provider<GetMasterDataUseCase> getMasterDataUseCaseProvider;

  private final Provider<FormDataSource> formDataSourceProvider;

  public DynamicFormViewModel_Factory(
      Provider<GetFormConfigurationUseCase> getFormConfigurationUseCaseProvider,
      Provider<GetMasterDataUseCase> getMasterDataUseCaseProvider,
      Provider<FormDataSource> formDataSourceProvider) {
    this.getFormConfigurationUseCaseProvider = getFormConfigurationUseCaseProvider;
    this.getMasterDataUseCaseProvider = getMasterDataUseCaseProvider;
    this.formDataSourceProvider = formDataSourceProvider;
  }

  @Override
  public DynamicFormViewModel get() {
    return newInstance(getFormConfigurationUseCaseProvider.get(), getMasterDataUseCaseProvider.get(), formDataSourceProvider.get());
  }

  public static DynamicFormViewModel_Factory create(
      Provider<GetFormConfigurationUseCase> getFormConfigurationUseCaseProvider,
      Provider<GetMasterDataUseCase> getMasterDataUseCaseProvider,
      Provider<FormDataSource> formDataSourceProvider) {
    return new DynamicFormViewModel_Factory(getFormConfigurationUseCaseProvider, getMasterDataUseCaseProvider, formDataSourceProvider);
  }

  public static DynamicFormViewModel newInstance(
      GetFormConfigurationUseCase getFormConfigurationUseCase,
      GetMasterDataUseCase getMasterDataUseCase, FormDataSource formDataSource) {
    return new DynamicFormViewModel(getFormConfigurationUseCase, getMasterDataUseCase, formDataSource);
  }
}
