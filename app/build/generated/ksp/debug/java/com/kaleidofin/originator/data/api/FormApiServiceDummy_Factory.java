package com.kaleidofin.originator.data.api;

import com.google.gson.Gson;
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
public final class FormApiServiceDummy_Factory implements Factory<FormApiServiceDummy> {
  private final Provider<Gson> gsonProvider;

  public FormApiServiceDummy_Factory(Provider<Gson> gsonProvider) {
    this.gsonProvider = gsonProvider;
  }

  @Override
  public FormApiServiceDummy get() {
    return newInstance(gsonProvider.get());
  }

  public static FormApiServiceDummy_Factory create(Provider<Gson> gsonProvider) {
    return new FormApiServiceDummy_Factory(gsonProvider);
  }

  public static FormApiServiceDummy newInstance(Gson gson) {
    return new FormApiServiceDummy(gson);
  }
}
