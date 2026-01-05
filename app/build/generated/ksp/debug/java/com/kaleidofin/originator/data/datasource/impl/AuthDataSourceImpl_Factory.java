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
public final class AuthDataSourceImpl_Factory implements Factory<AuthDataSourceImpl> {
  @Override
  public AuthDataSourceImpl get() {
    return newInstance();
  }

  public static AuthDataSourceImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AuthDataSourceImpl newInstance() {
    return new AuthDataSourceImpl();
  }

  private static final class InstanceHolder {
    private static final AuthDataSourceImpl_Factory INSTANCE = new AuthDataSourceImpl_Factory();
  }
}
