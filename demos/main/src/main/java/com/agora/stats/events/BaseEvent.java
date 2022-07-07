package com.agora.stats.events;
import com.agora.stats.events.IEvent;
import org.json.JSONObject;
import com.agora.stats.events.EventDataStorage;
public class BaseEvent implements IEvent
{
  public static final String TYPE_NAME = "Base";
  public static final int INVALID_ID= -1;

  public static final String EVENT_TIMESTAMP = "lts";
  public static final String EVENT_VID = "vid";
  public static final String EVENT_TOKEN = "token";
  public static final String EVENT_START_ID = "startId";
  public static final String EVENT_PLAYER_ID = "playerId";
  public static final String EVENT_STREAM_ID = "streamId";

  public static final String EVENT_URL = "url";

  protected EventDataStorage dataStorage;

  public BaseEvent(){
    this.dataStorage = new EventDataStorage();
  }

  @Override
  public int getEventId(){return INVALID_ID;};

  @Override
  public String getType(){return TYPE_NAME;};


  @Override
  public String toString(){
    return this.dataStorage.toString();
  }

  public void setTimestamp(long value){
    this.dataStorage.put(EVENT_TIMESTAMP, value);
  }

  public void setVid(int value){
    this.dataStorage.put(EVENT_VID, value);
  }

  public void setToken(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_TOKEN, value);
    }
  }

  public void setStartId(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_START_ID, value);
    }
  }

  public void setPlayerId(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_PLAYER_ID, value);
    }
  }

  public void setStreamId(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_STREAM_ID, value);
    }
  }

  public void setUrl(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_URL, value);
    }
  }
}
