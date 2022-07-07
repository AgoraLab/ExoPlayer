package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
import org.json.JSONException;

public class PauseEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerPause";

  public static final String EVENT_POS = "pos";

  public static final int EVENT_ID = 9880;


  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setPos(final long value)  {
      this.dataStorage.put(EVENT_POS, value);
  }
}
