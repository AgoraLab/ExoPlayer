package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class PlayEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerPlay";

  public static final String EVENT_PROTOCOL = "protocol";
  public static final int EVENT_ID = 9879;

  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setProtocol(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_PROTOCOL, value);
    }
  }
}
