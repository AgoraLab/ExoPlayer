package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class SeekingEvent extends BaseEvent{
  public static final String TYPE_NAME = "seeking";

  public static final String EVENT_CURRENT_POS = "curpos";
  public static final String EVENT_SEEK_POS = "seekpos";

  public static final int EVENT_ID = 9903;

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
}
