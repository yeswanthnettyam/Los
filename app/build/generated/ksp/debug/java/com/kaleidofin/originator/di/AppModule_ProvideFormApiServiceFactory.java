package com.kaleidofin.originator.di;

import com.google.gson.Gson;
import com.kaleidofin.originator.data.api.FormApiService;
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
public final class AppModule_ProvideFormApiServiceFactory implements Factory<FormApiService> {
  private final Provider<Gson> gsonProvider;

  public AppModule_ProvideFormApiServiceFactory(Provider<Gson> gsonProvider) {
    this.gsonProvider = gsonProvider;
  }

  @Override
  public FormApiService get() {
    return provideFormApiService(gsonProvider.get());
  }

  public static AppModule_ProvideFormApiServiceFactory create(Provider<Gson> gsonProvider) {
    return new AppModule_ProvideFormApiServiceFactory(gsonProvider);
  }

  public static FormApiService provideFormApiService(Gson gson) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFormApiService(gson));
  }
}
