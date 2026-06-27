package org.afetch;

public enum ConfigKey {
  RESOLUTION("resolution"),
  KERNEL("kernel"),
  BRAND("brand"),
  VENDOR("vendor"),
  DE("DE"),
  WM("WM"),
  CPU("CPU"),
  GPU("GPU"),
  MEMORY("memory"),
  STORAGE("storage"),
  CPU_ARCH("CPUArch"),
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

