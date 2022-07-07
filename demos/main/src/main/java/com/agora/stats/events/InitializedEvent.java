package com.agora.stats.events;

import com.agora.stats.events.BaseEvent;
import org.json.JSONException;

public class InitializedEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerInitialized";

  public static final String EVENT_MEDIA_PLAYER_VERSION = "mpver";
  public static final String EVENT_PLUGIN_VERSION = "plugver";

  public static final int EVENT_ID = 9878;
  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public InitializedEvent(final String mpVersion, final String pluginVersion) {

    if(null != mpVersion){
      this.dataStorage.put(EVENT_MEDIA_PLAYER_VERSION, mpVersion);
    }

    if(null != pluginVersion){
      this.dataStorage.put(EVENT_PLUGIN_VERSION, pluginVersion);
    }
  }



}
