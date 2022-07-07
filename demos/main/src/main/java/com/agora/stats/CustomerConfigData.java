package com.agora.stats;

public class CustomerConfigData {

  private int vid;
  private String token;
  private String environmentToken;

  public CustomerConfigData(){
  }

  public void setVid(int value){
    this.vid = value;
  }

  public void setToken(String value){
    this.token = value;
  }

  public void setEnvironmentToken(String value){
    this.environmentToken = value;
  }

  public int getVid(){
    return this.vid;
  }

  public String getToken(){
    return this.token;
  }

  public String getEnvironmentToken(){
    return this.environmentToken;
  }

}
