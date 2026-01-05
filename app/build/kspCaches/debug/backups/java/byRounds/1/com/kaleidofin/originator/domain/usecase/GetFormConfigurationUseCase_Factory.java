package com.kaleidofin.originator.domain.usecase;

import com.kaleidofin.originator.domain.repository.FormRepository;
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
public final class GetFormConfigurationUseCase_Factory implements Factory<GetFormConfigurationUseCase> {
  private final Provider<FormRepository> formRepositoryProvider;

  public GetFormConfigurationUseCase_Factory(Provider<FormRepository> formRepositoryProvider) {
    this.formRepositoryProvider = formRepositoryProvider;
  }

  @Override
  public GetFormConfigurationUseCase get() {
    return newInstance(formRepositoryProvider.get());
  }

  public static GetFormConfigurationUseCase_Factory create(
      Provider<FormRepository> formRepositoryProvider) {
    return new GetFormConfigurationUseCase_Factory(formRepositoryProvider);
  }

  public static GetFormConfigurationUseCase newInstance(FormRepository formRepository) {
    return new GetFormConfigurationUseCase(formRepository);
  }
}
