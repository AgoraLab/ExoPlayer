package com.agora.stats.common;

import com.google.android.exoplayer2.util.Log;

public class Logger {

  public interface LoggerCallback {

    void outputLog(String type, String tag, String message);
  }

  public static LoggerCallback loggerCallback = null;

  public static void setLoggerCallback(LoggerCallback loggerCallback){
    Logger.loggerCallback = loggerCallback;
  }

  public static void d(String tag, String message){
    if(null != Logger.loggerCallback){
      Logger.loggerCallback.outputLog("debug", tag, message);
    }
  }

  public static void e(String tag, String message){
    if(null != Logger.loggerCallback){
      Logger.loggerCallback.outputLog("error", tag, message);
    }
  }



}
