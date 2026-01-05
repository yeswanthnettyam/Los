package com.kaleidofin.originator.data.datasource.impl;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class HomeDataSourceImpl_Factory implements Factory<HomeDataSourceImpl> {
  @Override
  public HomeDataSourceImpl get() {
    return newInstance();
  }

  public static HomeDataSourceImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HomeDataSourceImpl newInstance() {
    return new HomeDataSourceImpl();
  }

  private static final class InstanceHolder {
    private static final HomeDataSourceImpl_Factory INSTANCE = new HomeDataSourceImpl_Factory();
  }
}
