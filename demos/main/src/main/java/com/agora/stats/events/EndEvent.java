package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;

public class EndEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerEnd";

  public static final int EVENT_ID = 9886;
  @Override
  public int getEventId(){return EVENT_ID;};

  public static final String EVENT_DURATION = "durationTime";

  @Override
  public String getType(){return TYPE_NAME;};

  public void setDuration(int value){
    this.dataStorage.put(EVENT_DURATION, value);
  }
}
