package com.agora.stats.common;

public class ElapseTimer {

  private String name;
  private long startTime;

  public ElapseTimer(String name){
    this.name = name;
    startTime = System.currentTimeMillis();
  }

  public void close(){
    long elapseTime = System.currentTimeMillis() - this.startTime;
    Logger.d("ElapseTimer", "timer: " + name + " elapse time:" + elapseTime + " ms");
  }

}
