package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;

public class ErrorEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerSeeking";

  public static final String EVENT_ERROR_MSG = "errormsg";

  public static final int EVENT_ID = 9901;


  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setErrorMsg(String value){
    if(null != value){
      this.dataStorage.put(EVENT_ERROR_MSG, value);
    }
  }

}
