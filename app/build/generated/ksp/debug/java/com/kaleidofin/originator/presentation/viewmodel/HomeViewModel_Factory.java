package com.kaleidofin.originator.presentation.viewmodel;

import com.kaleidofin.originator.domain.usecase.GetHomeActionsUseCase;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<GetHomeActionsUseCase> getHomeActionsUseCaseProvider;

  public HomeViewModel_Factory(Provider<GetHomeActionsUseCase> getHomeActionsUseCaseProvider) {
    this.getHomeActionsUseCaseProvider = getHomeActionsUseCaseProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(getHomeActionsUseCaseProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<GetHomeActionsUseCase> getHomeActionsUseCaseProvider) {
    return new HomeViewModel_Factory(getHomeActionsUseCaseProvider);
  }

  public static HomeViewModel newInstance(GetHomeActionsUseCase getHomeActionsUseCase) {
    return new HomeViewModel(getHomeActionsUseCase);
  }
}
