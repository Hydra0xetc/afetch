package org.afetch;

public enum ConfigKey {
  LOGO("logo"),
  FOOTER("footer"),
  HEADER("header"),
  OS("os"),
  LOCAL_IP("localIP"),
  HOST("host"),
  DPI("dpi"),
  RESOLUTION("resolution"),
  KERNEL("kernel"),
  BRAND("brand"),
  DE("de"),
  WM("wm"),
  CPU("cpu"),
  GPU("gpu"),
  MEMORY("memory"),
  SWAP("swap"),
  STORAGE("storage"),
  ABI("abi"),
  BATTERY("battery"),
  APK_COUNT("apkCount"),
  PACKAGE_COUNT("packageCount"),
  UPTIME("uptime");

  private final String key;

  ConfigKey(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public static ConfigKey fromKey(String key) {
    for (ConfigKey cfg : values()) {
      if (cfg.getKey().equals(key)) {
        return cfg;
      }
    }

    throw new IllegalArgumentException(
      "Unknown config key: " + key
    );
  }
}
