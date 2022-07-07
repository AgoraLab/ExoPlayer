package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class FirstframerenderedEvent extends  BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerFirstframerendered";

  public static final String EVENT_COST_TIME = "costtimeMs";
  public static final int EVENT_ID = 9899;
  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setCostTime(final long value){
    this.dataStorage.put(EVENT_COST_TIME, value);
  }
  public long getCostTime(){
    return this.dataStorage.getLong(EVENT_COST_TIME);
  }
}
