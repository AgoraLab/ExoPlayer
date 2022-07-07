package com.agora.stats.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
  public static String MD5(String input) {
    if(input == null || input.length() == 0) {
      return null;
    }
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(input.getBytes());
      byte[] byteArray = md5.digest();

      StringBuilder sb = new StringBuilder();
      for (byte b : byteArray) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }
}
