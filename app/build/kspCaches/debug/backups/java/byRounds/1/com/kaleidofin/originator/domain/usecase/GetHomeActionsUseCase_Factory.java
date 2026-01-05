package com.kaleidofin.originator.domain.usecase;

import com.kaleidofin.originator.domain.repository.HomeRepository;
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
public final class GetHomeActionsUseCase_Factory implements Factory<GetHomeActionsUseCase> {
  private final Provider<HomeRepository> homeRepositoryProvider;

  public GetHomeActionsUseCase_Factory(Provider<HomeRepository> homeRepositoryProvider) {
    this.homeRepositoryProvider = homeRepositoryProvider;
  }

  @Override
  public GetHomeActionsUseCase get() {
    return newInstance(homeRepositoryProvider.get());
  }

  public static GetHomeActionsUseCase_Factory create(
      Provider<HomeRepository> homeRepositoryProvider) {
    return new GetHomeActionsUseCase_Factory(homeRepositoryProvider);
  }

  public static GetHomeActionsUseCase newInstance(HomeRepository homeRepository) {
    return new GetHomeActionsUseCase(homeRepository);
  }
}
