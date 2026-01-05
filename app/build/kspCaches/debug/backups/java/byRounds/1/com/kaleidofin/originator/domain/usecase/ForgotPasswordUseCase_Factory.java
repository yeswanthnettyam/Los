package com.kaleidofin.originator.domain.usecase;

import com.kaleidofin.originator.domain.repository.AuthRepository;
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
public final class ForgotPasswordUseCase_Factory implements Factory<ForgotPasswordUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public ForgotPasswordUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ForgotPasswordUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static ForgotPasswordUseCase_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new ForgotPasswordUseCase_Factory(authRepositoryProvider);
  }

  public static ForgotPasswordUseCase newInstance(AuthRepository authRepository) {
    return new ForgotPasswordUseCase(authRepository);
  }
}
