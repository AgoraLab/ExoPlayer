package com.agora.stats.events;

import com.agora.stats.events.BaseEvent;

public class DestroyEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerDestroy";
  public static final int EVENT_ID = 9887;

  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};
}
