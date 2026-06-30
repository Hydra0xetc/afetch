package org.afetch;

public enum ConfigKey {
  LOGO("logo"),
  LOCAL_IP("localIP"),
  HOST("host"),
  DPI("dpi"),
  RESOLUTION("resolution"),
  KERNEL("kernel"),
  BRAND("brand"),
  DE("DE"),
  WM("WM"),
  CPU("CPU"),
  GPU("GPU"),
  MEMORY("memory"),
  SWAP("swap"),
  STORAGE("storage"),
  ABI("ABI"),
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
}

