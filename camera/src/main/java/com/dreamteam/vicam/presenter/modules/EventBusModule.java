package com.dreamteam.vicam.presenter.modules;

import de.greenrobot.event.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by fsommar on 2/5/14.
 */
@Module(
    injects = {
    },
    complete = false
)
public class EventBusModule {

  @Provides
  @Singleton
  public EventBus provideEventBus() {
    return new EventBus();
  }
}
