package com.agora.stats.common;

import java.util.Random;

public class UUID
{
  public static String generateUUID() {
    final Random random = new Random();
    String str = "";
    for (int i = 0; i < 36; ++i) {
      final int nextInt = random.nextInt(16);
      final char char1;
      if ((char1 = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".charAt(i)) == 'x') {
        str += Integer.toHexString(nextInt);
      }
      else if (char1 == 'y') {
        str += Integer.toHexString((nextInt & 0x3) | 0x8);
      }
      else {
        str += char1;
      }
    }
    return str;
  }

  public static String shortUUID() {
    final String string = "000000" + Integer.toString(new Random().nextInt(), 36);
    return string.substring(string.length() - 6);
  }
}

