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
public final class GetMasterDataUseCase_Factory implements Factory<GetMasterDataUseCase> {
  private final Provider<FormRepository> formRepositoryProvider;

  public GetMasterDataUseCase_Factory(Provider<FormRepository> formRepositoryProvider) {
    this.formRepositoryProvider = formRepositoryProvider;
  }

  @Override
  public GetMasterDataUseCase get() {
    return newInstance(formRepositoryProvider.get());
  }

  public static GetMasterDataUseCase_Factory create(
      Provider<FormRepository> formRepositoryProvider) {
    return new GetMasterDataUseCase_Factory(formRepositoryProvider);
  }

  public static GetMasterDataUseCase newInstance(FormRepository formRepository) {
    return new GetMasterDataUseCase(formRepository);
  }
}
