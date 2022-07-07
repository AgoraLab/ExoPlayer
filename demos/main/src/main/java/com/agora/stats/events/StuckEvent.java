package com.agora.stats.events;

public class StuckEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerStuck";

  public static final String EVENT_DURATION = "durationMs";

  public static final int EVENT_ID = 9896;
  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setDuration(long value){
    this.dataStorage.put(EVENT_DURATION, value);
  }

  public long getDuration(){
    return this.dataStorage.getLong(EVENT_DURATION);
  }
}
