package com.kaleidofin.originator.presentation.viewmodel;

import com.kaleidofin.originator.domain.usecase.ForgotPasswordUseCase;
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
public final class ForgotPasswordViewModel_Factory implements Factory<ForgotPasswordViewModel> {
  private final Provider<ForgotPasswordUseCase> forgotPasswordUseCaseProvider;

  public ForgotPasswordViewModel_Factory(
      Provider<ForgotPasswordUseCase> forgotPasswordUseCaseProvider) {
    this.forgotPasswordUseCaseProvider = forgotPasswordUseCaseProvider;
  }

  @Override
  public ForgotPasswordViewModel get() {
    return newInstance(forgotPasswordUseCaseProvider.get());
  }

  public static ForgotPasswordViewModel_Factory create(
      Provider<ForgotPasswordUseCase> forgotPasswordUseCaseProvider) {
    return new ForgotPasswordViewModel_Factory(forgotPasswordUseCaseProvider);
  }

  public static ForgotPasswordViewModel newInstance(ForgotPasswordUseCase forgotPasswordUseCase) {
    return new ForgotPasswordViewModel(forgotPasswordUseCase);
  }
}
