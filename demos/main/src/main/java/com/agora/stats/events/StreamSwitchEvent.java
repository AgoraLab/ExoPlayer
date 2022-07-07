package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class StreamSwitchEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerStreamswitch";

  public static final String EVENT_NEW_STREAM_ID = "newStreamid";

  public static final int EVENT_ID = 9900;

  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setNewStreamId(String value){
    if(null != value){
      this.dataStorage.put(EVENT_NEW_STREAM_ID, value);
    }
  }
}
