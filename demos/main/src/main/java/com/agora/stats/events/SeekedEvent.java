package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class SeekedEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerSeeked";

  public static final String EVENT_CURRENT_POS = "curpos";
  public static final String EVENT_SEEK_POS = "seekpos";

  public static final String EVENT_DURATION = "durationMs";

  public static final int EVENT_ID = 9905;
  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};


  public void setCurrentPos(long value){
    this.dataStorage.put(EVENT_CURRENT_POS, value);
  }
  public void setSeekPos(long value) {
    this.dataStorage.put(EVENT_SEEK_POS, value);
  }
  public void setDuration(long value){
    this.dataStorage.put(EVENT_DURATION, value);
  }
}
