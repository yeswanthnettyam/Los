package com.kaleidofin.originator;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.gson.Gson;
import com.kaleidofin.originator.data.api.FormApiService;
import com.kaleidofin.originator.data.datasource.AuthDataSource;
import com.kaleidofin.originator.data.datasource.FormDataSource;
import com.kaleidofin.originator.data.datasource.HomeDataSource;
import com.kaleidofin.originator.di.AppModule_ProvideAuthDataSourceFactory;
import com.kaleidofin.originator.di.AppModule_ProvideAuthRepositoryFactory;
import com.kaleidofin.originator.di.AppModule_ProvideFormApiServiceFactory;
import com.kaleidofin.originator.di.AppModule_ProvideFormDataSourceFactory;
import com.kaleidofin.originator.di.AppModule_ProvideFormRepositoryFactory;
import com.kaleidofin.originator.di.AppModule_ProvideGsonFactory;
import com.kaleidofin.originator.di.AppModule_ProvideHomeDataSourceFactory;
import com.kaleidofin.originator.di.AppModule_ProvideHomeRepositoryFactory;
import com.kaleidofin.originator.domain.repository.AuthRepository;
import com.kaleidofin.originator.domain.repository.FormRepository;
import com.kaleidofin.originator.domain.repository.HomeRepository;
import com.kaleidofin.originator.domain.usecase.ForgotPasswordUseCase;
import com.kaleidofin.originator.domain.usecase.GetFormConfigurationUseCase;
import com.kaleidofin.originator.domain.usecase.GetHomeActionsUseCase;
import com.kaleidofin.originator.domain.usecase.GetMasterDataUseCase;
import com.kaleidofin.originator.domain.usecase.LoginUseCase;
import com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel;
import com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel_HiltModules;
import com.kaleidofin.originator.presentation.viewmodel.ForgotPasswordViewModel;
import com.kaleidofin.originator.presentation.viewmodel.ForgotPasswordViewModel_HiltModules;
import com.kaleidofin.originator.presentation.viewmodel.HomeViewModel;
import com.kaleidofin.originator.presentation.viewmodel.HomeViewModel_HiltModules;
import com.kaleidofin.originator.presentation.viewmodel.LoginViewModel;
import com.kaleidofin.originator.presentation.viewmodel.LoginViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerOriginatorApplication_HiltComponents_SingletonC {
  private DaggerOriginatorApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static OriginatorApplication_HiltComponents.SingletonC create() {
    return new Builder().build();
  }

  public static final class Builder {
    private Builder() {
    }

    /**
     * @deprecated This module is declared, but an instance is not used in the component. This method is a no-op. For more, see https://dagger.dev/unused-modules.
     */
    @Deprecated
    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public OriginatorApplication_HiltComponents.SingletonC build() {
      return new SingletonCImpl();
    }
  }

  private static final class ActivityRetainedCBuilder implements OriginatorApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements OriginatorApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements OriginatorApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements OriginatorApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements OriginatorApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements OriginatorApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements OriginatorApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public OriginatorApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends OriginatorApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends OriginatorApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends OriginatorApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends OriginatorApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(4).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_DynamicFormViewModel, DynamicFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_ForgotPasswordViewModel, ForgotPasswordViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_LoginViewModel, LoginViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_kaleidofin_originator_presentation_viewmodel_HomeViewModel = "com.kaleidofin.originator.presentation.viewmodel.HomeViewModel";

      static String com_kaleidofin_originator_presentation_viewmodel_ForgotPasswordViewModel = "com.kaleidofin.originator.presentation.viewmodel.ForgotPasswordViewModel";

      static String com_kaleidofin_originator_presentation_viewmodel_DynamicFormViewModel = "com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel";

      static String com_kaleidofin_originator_presentation_viewmodel_LoginViewModel = "com.kaleidofin.originator.presentation.viewmodel.LoginViewModel";

      @KeepFieldType
      HomeViewModel com_kaleidofin_originator_presentation_viewmodel_HomeViewModel2;

      @KeepFieldType
      ForgotPasswordViewModel com_kaleidofin_originator_presentation_viewmodel_ForgotPasswordViewModel2;

      @KeepFieldType
      DynamicFormViewModel com_kaleidofin_originator_presentation_viewmodel_DynamicFormViewModel2;

      @KeepFieldType
      LoginViewModel com_kaleidofin_originator_presentation_viewmodel_LoginViewModel2;
    }
  }

  private static final class ViewModelCImpl extends OriginatorApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<DynamicFormViewModel> dynamicFormViewModelProvider;

    private Provider<ForgotPasswordViewModel> forgotPasswordViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private GetFormConfigurationUseCase getFormConfigurationUseCase() {
      return new GetFormConfigurationUseCase(singletonCImpl.provideFormRepositoryProvider.get());
    }

    private GetMasterDataUseCase getMasterDataUseCase() {
      return new GetMasterDataUseCase(singletonCImpl.provideFormRepositoryProvider.get());
    }

    private ForgotPasswordUseCase forgotPasswordUseCase() {
      return new ForgotPasswordUseCase(singletonCImpl.provideAuthRepositoryProvider.get());
    }

    private GetHomeActionsUseCase getHomeActionsUseCase() {
      return new GetHomeActionsUseCase(singletonCImpl.provideHomeRepositoryProvider.get());
    }

    private LoginUseCase loginUseCase() {
      return new LoginUseCase(singletonCImpl.provideAuthRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.dynamicFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.forgotPasswordViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(4).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_DynamicFormViewModel, ((Provider) dynamicFormViewModelProvider)).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_ForgotPasswordViewModel, ((Provider) forgotPasswordViewModelProvider)).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_kaleidofin_originator_presentation_viewmodel_LoginViewModel, ((Provider) loginViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_kaleidofin_originator_presentation_viewmodel_ForgotPasswordViewModel = "com.kaleidofin.originator.presentation.viewmodel.ForgotPasswordViewModel";

      static String com_kaleidofin_originator_presentation_viewmodel_HomeViewModel = "com.kaleidofin.originator.presentation.viewmodel.HomeViewModel";

      static String com_kaleidofin_originator_presentation_viewmodel_DynamicFormViewModel = "com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel";

      static String com_kaleidofin_originator_presentation_viewmodel_LoginViewModel = "com.kaleidofin.originator.presentation.viewmodel.LoginViewModel";

      @KeepFieldType
      ForgotPasswordViewModel com_kaleidofin_originator_presentation_viewmodel_ForgotPasswordViewModel2;

      @KeepFieldType
      HomeViewModel com_kaleidofin_originator_presentation_viewmodel_HomeViewModel2;

      @KeepFieldType
      DynamicFormViewModel com_kaleidofin_originator_presentation_viewmodel_DynamicFormViewModel2;

      @KeepFieldType
      LoginViewModel com_kaleidofin_originator_presentation_viewmodel_LoginViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel 
          return (T) new DynamicFormViewModel(viewModelCImpl.getFormConfigurationUseCase(), viewModelCImpl.getMasterDataUseCase(), singletonCImpl.provideFormDataSourceProvider.get());

          case 1: // com.kaleidofin.originator.presentation.viewmodel.ForgotPasswordViewModel 
          return (T) new ForgotPasswordViewModel(viewModelCImpl.forgotPasswordUseCase());

          case 2: // com.kaleidofin.originator.presentation.viewmodel.HomeViewModel 
          return (T) new HomeViewModel(viewModelCImpl.getHomeActionsUseCase());

          case 3: // com.kaleidofin.originator.presentation.viewmodel.LoginViewModel 
          return (T) new LoginViewModel(viewModelCImpl.loginUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends OriginatorApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends OriginatorApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends OriginatorApplication_HiltComponents.SingletonC {
    private final SingletonCImpl singletonCImpl = this;

    private Provider<Gson> provideGsonProvider;

    private Provider<FormApiService> provideFormApiServiceProvider;

    private Provider<FormDataSource> provideFormDataSourceProvider;

    private Provider<FormRepository> provideFormRepositoryProvider;

    private Provider<AuthDataSource> provideAuthDataSourceProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<HomeDataSource> provideHomeDataSourceProvider;

    private Provider<HomeRepository> provideHomeRepositoryProvider;

    private SingletonCImpl() {

      initialize();

    }

    @SuppressWarnings("unchecked")
    private void initialize() {
      this.provideGsonProvider = DoubleCheck.provider(new SwitchingProvider<Gson>(singletonCImpl, 3));
      this.provideFormApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<FormApiService>(singletonCImpl, 2));
      this.provideFormDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<FormDataSource>(singletonCImpl, 1));
      this.provideFormRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<FormRepository>(singletonCImpl, 0));
      this.provideAuthDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<AuthDataSource>(singletonCImpl, 5));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 4));
      this.provideHomeDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<HomeDataSource>(singletonCImpl, 7));
      this.provideHomeRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<HomeRepository>(singletonCImpl, 6));
    }

    @Override
    public void injectOriginatorApplication(OriginatorApplication originatorApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.kaleidofin.originator.domain.repository.FormRepository 
          return (T) AppModule_ProvideFormRepositoryFactory.provideFormRepository(singletonCImpl.provideFormDataSourceProvider.get());

          case 1: // com.kaleidofin.originator.data.datasource.FormDataSource 
          return (T) AppModule_ProvideFormDataSourceFactory.provideFormDataSource(singletonCImpl.provideFormApiServiceProvider.get());

          case 2: // com.kaleidofin.originator.data.api.FormApiService 
          return (T) AppModule_ProvideFormApiServiceFactory.provideFormApiService(singletonCImpl.provideGsonProvider.get());

          case 3: // com.google.gson.Gson 
          return (T) AppModule_ProvideGsonFactory.provideGson();

          case 4: // com.kaleidofin.originator.domain.repository.AuthRepository 
          return (T) AppModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideAuthDataSourceProvider.get());

          case 5: // com.kaleidofin.originator.data.datasource.AuthDataSource 
          return (T) AppModule_ProvideAuthDataSourceFactory.provideAuthDataSource();

          case 6: // com.kaleidofin.originator.domain.repository.HomeRepository 
          return (T) AppModule_ProvideHomeRepositoryFactory.provideHomeRepository(singletonCImpl.provideHomeDataSourceProvider.get());

          case 7: // com.kaleidofin.originator.data.datasource.HomeDataSource 
          return (T) AppModule_ProvideHomeDataSourceFactory.provideHomeDataSource();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
