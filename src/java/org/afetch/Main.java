package org.afetch;

import android.app.IActivityManager;
import android.os.BatteryManager;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.util.DisplayMetrics;

import android.view.WindowManager;
import android.view.Display;

import android.os.ServiceManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.Inet4Address;

import java.util.Enumeration;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import java.lang.reflect.Method;

import org.json.JSONObject;

import static org.afetch.Logger.Level.*;

/*
 *   ;,           ,;
 *    ';,.-----.,;'
 *   ,'           ',
 *  /    O     O    \
 * |                 |
 * '-----------------'
 *  **** ANDROID *****
 */

public class Main {
  private static final String TAG          = "Main";
  private static final String PREFIX       = System.getenv("PREFIX");
  private static final String PROGRAM_NAME = "afetch";
  private static final String VERSION      = "1.0.1";
  private static final String GREEN_BOLD   = "\u001B[1;32m";
  private static final String YELLOW_BOLD  = "\u001B[1;33m";
  private static final String RED_BOLD     = "\u001B[1;31m";
  private static final String WHITE_BOLD   = "\u001B[1;37m";
  private static final String RESET        = "\u001B[0m";

  private static Logger logger = Logger.getInstance();

  // NOTE: I Think better if the logo have bigger eye LUL ;v
  private static void printLogo() {
      System.out.println(GREEN_BOLD + """
     ;,                       ,;
      ';,.-----------------.,;'
      ,'                     ',
    ,'                         ',
   /      O               O      \\
  |                               |
  |                               |
  |                               |
  |                               |
  '-------------------------------'
  """ + WHITE_BOLD + """
  ******       ANDROID       ******""" + RESET);
  }

  private static String getLocalIP() {
    try {
      Enumeration<NetworkInterface> interfaces =
        NetworkInterface.getNetworkInterfaces();

      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();

        if (!iface.isUp() || iface.isLoopback()) {
          continue;
        }

        Enumeration<InetAddress> addresses =
          iface.getInetAddresses();

        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();

          if (addr.isLoopbackAddress()) {
            continue;
          }

          if (addr instanceof Inet4Address) {
            return String.format(
              "%s ("+GREEN_BOLD+"%s"+RESET+")",
              addr.getHostAddress(),
              iface.getName()
            );
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return "Unknown";
  }

  private static String getPackageInfo() {
    StringBuilder result = new StringBuilder();

    // There are only two package managers in Termux, pacman and apt/dpkg
    int pacmanCount = countPacman(PREFIX);
    if (pacmanCount >= 0) {
      appendResult(result, pacmanCount, "pacman");
    }

    int dpkgCount = countDpkg(PREFIX);
    if (dpkgCount >= 0) {
      appendResult(result, dpkgCount, "dpkg");
    }

    return result.length() > 0 ? result.toString() : "Unknown";
  }

  private static void appendResult(StringBuilder sb, int count, String name) {
    if (sb.length() > 0) sb.append(", ");
    sb.append(count).append(" (").append(name).append(")");
  }

  private static int countPacman(String PREFIX) {
    try {
      String basePath = PREFIX + "/var/lib/pacman/local";
      File dir = new File(basePath);
      File[] entries = dir.listFiles(File::isDirectory);
      return entries != null ? entries.length : -1;
    } catch (Exception e) {
      return -1;
    }
  }

  private static int countDpkg(String PREFIX) {
    try {
      String basePath = PREFIX + "/var/lib/dpkg/status";
      File statusFile = new File(basePath);
      if (!statusFile.exists()) return -1;

      BufferedReader reader = new BufferedReader(new FileReader(statusFile));
      int count = 0;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("Package: ")) count++;
      }
      reader.close();
      return count;
    } catch (Exception e) {
      return -1;
    }
  }

  private static String getApkCount(Context ctx) {
    try {
      PackageManager pm = ctx.getPackageManager();
      List<ApplicationInfo> apps = pm.getInstalledApplications(0);

      int total = apps.size();
      int systemApps = 0;
      int userApps = 0;

      for (ApplicationInfo app : apps) {
        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
          systemApps++;
        } else {
          userApps++;
        }
      }

      return String.format("%d (%d system, %d user)", total, systemApps, userApps);

    } catch (Exception e) {
      e.printStackTrace();
      return "Unknown";
    }
  }

  private static String batteryColor(int percent) {
    if (percent <= 20) {
      return RED_BOLD +
        String.valueOf(percent) + "%" + RESET;
    }

    if (percent <= 50) {
      return YELLOW_BOLD +
        String.valueOf(percent) + "%" + RESET;
    }

    return GREEN_BOLD +
      String.valueOf(percent) + "%" + RESET;
  }

  private static String batteryStatus(long status) {
    return switch ((int) status) {
      case BatteryManager.BATTERY_STATUS_UNKNOWN ->
        "Unknown";

      case BatteryManager.BATTERY_STATUS_CHARGING ->
        "Charging";

      case BatteryManager.BATTERY_STATUS_DISCHARGING ->
        "Discharging";

      case BatteryManager.BATTERY_STATUS_NOT_CHARGING ->
        "Not charging";

      case BatteryManager.BATTERY_STATUS_FULL ->
        "Full";

      default ->
        "Invalid (" + status + ")";
    };
  }

  private static String getBatteryInfo() {
    try {
      IActivityManager am =
        IActivityManager.Stub.asInterface(
          ServiceManager.getService("activity")
      );

      Method registerReceiver = null;

      for (Method m : IActivityManager.class.getDeclaredMethods()) {
        if (m.getName().equals("registerReceiver")) {
          registerReceiver = m;
          break;
        }
      }

      if (registerReceiver == null) {
        throw new NoSuchMethodException(
          "registerReceiver not found"
        );
      }

      IntentFilter filter =
        new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

      Intent batteryIntent;

      if (registerReceiver.getParameterCount() == 7) {
        // Android 10
        batteryIntent = (Intent) registerReceiver.invoke(
            am,
            null,        // IApplicationThread
            null,        // calling package
            null,        // IIntentReceiver
            filter,
            null,        // permission
            0,           // userId
            0            // flags
        );
      } else if (registerReceiver.getParameterCount() == 8) {
        // Android 11+
        batteryIntent = (Intent) registerReceiver.invoke(
            am,
            null,        // IApplicationThread
            null,        // calling package
            null,        // callingFeatureId
            null,        // IIntentReceiver
            filter,
            null,        // permission
            0,           // userId
            0            // flags
        );
      } else {
        throw new UnsupportedOperationException(
          registerReceiver.toString()
        );
      }

      if (batteryIntent != null) {
        int level = batteryIntent.getIntExtra(
          BatteryManager.EXTRA_LEVEL,
          -1
        );

        int scale = batteryIntent.getIntExtra(
          BatteryManager.EXTRA_SCALE,
          100
        );

        int percent = level * 100 / scale;

        int status = batteryIntent.getIntExtra(
          BatteryManager.EXTRA_STATUS,
          BatteryManager.BATTERY_STATUS_UNKNOWN
        );

        int temp = batteryIntent.getIntExtra(
          BatteryManager.EXTRA_TEMPERATURE,
          0
        );

        String technology = batteryIntent.getStringExtra(
          BatteryManager.EXTRA_TECHNOLOGY
        );

        return String.format(
          "%s [%s | %.1f°C | %s]",
          batteryColor(percent),
          batteryStatus(status),
          temp / 10.0,
          technology
        );
      }

    } catch (Throwable e) {
      logger.log(WARN, TAG,
        "registerReceiver failed: " + e.getMessage()
      );
    }

    // Fallback to termux-battery-status if registerReceiver failed
    String exe = PREFIX + "/bin/termux-battery-status";

    if (new File(exe).exists()) {
      try {
        Process p = Runtime.getRuntime().exec(exe);

        BufferedReader br = new BufferedReader(
          new InputStreamReader(p.getInputStream())
        );

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
          sb.append(line);
        }

        JSONObject obj = new JSONObject(sb.toString());

        int percent = obj.getInt("percentage");

        return String.format(
          "%s [%s | %.1f°C | %s]",
          batteryColor(percent),
          obj.getString("status").toLowerCase(),
          obj.getDouble("temperature"),
          obj.getString("technology")
        );

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return "Unknown";
  }

  private static String getAndroidVersion() {
    return Build.VERSION.RELEASE;
  }

  private static int getAndroidApiLevel() {
    return Build.VERSION.SDK_INT;
  }

  private static String getAndroidCodename() {
    int api = Build.VERSION.SDK_INT;

    switch (api) {
      case 1:  return "Base";
      case 2:  return "Base_1_1";
      case 3:  return "Cupcake";
      case 4:  return "Donut";
      case 5:
      case 6:
      case 7:  return "Eclair";
      case 8:  return "Froyo";
      case 9:
      case 10: return "Gingerbread";
      case 11:
      case 12:
      case 13: return "Honeycomb";
      case 14:
      case 15: return "Ice Cream Sandwich";
      case 16:
      case 17:
      case 18: return "Jelly Bean";
      case 19:
      case 20: return "KitKat";
      case 21:
      case 22: return "Lollipop";
      case 23: return "Marshmallow";
      case 24:
      case 25: return "Nougat";
      case 26:
      case 27: return "Oreo";
      case 28: return "Pie";
      case 29: return "Android 10 (Quince Tart)";
      case 30: return "Android 11 (Red Velvet Cake)";
      case 31:
      case 32: return "Android 12 (Snow Cone)";
      case 33: return "Android 13 (Tiramisu)";
      case 34: return "Android 14 (Upside Down Cake)";
      case 35: return "Android 15 (Vanilla Ice Cream)";
      case 36: return "Android 16 (Baklava)";
      default: return "Unknown";
    }
  }

  private static String getGpuInfo() {
    EGLDisplay display = EGL14.EGL_NO_DISPLAY;
    EGLContext context = EGL14.EGL_NO_CONTEXT;
    EGLSurface surface = EGL14.EGL_NO_SURFACE;

    try {
      display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

      if (display == EGL14.EGL_NO_DISPLAY)
        return "Unknown";

      int[] version = new int[2];

      if (!EGL14.eglInitialize(
            display,
            version, 0,
            version, 1)) {

        return "Unknown";
      }

      int[] configAttribs = {
        EGL14.EGL_RENDERABLE_TYPE,
        EGL14.EGL_OPENGL_ES2_BIT,

        EGL14.EGL_SURFACE_TYPE,
        EGL14.EGL_PBUFFER_BIT,

        EGL14.EGL_NONE
      };

      EGLConfig[] configs = new EGLConfig[1];
      int[] numConfigs = new int[1];

      if (!EGL14.eglChooseConfig(
            display,
            configAttribs, 0,
            configs, 0,
            configs.length,
            numConfigs, 0)) {

        return "Unknown";
      }

      int[] contextAttribs = {
        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL14.EGL_NONE
      };

      context = EGL14.eglCreateContext(
        display,
        configs[0],
        EGL14.EGL_NO_CONTEXT,
        contextAttribs, 0
      );

      int[] surfaceAttribs = {
        EGL14.EGL_WIDTH, 1,
        EGL14.EGL_HEIGHT, 1,
        EGL14.EGL_NONE
      };

      surface = EGL14.eglCreatePbufferSurface(
        display,
        configs[0],
        surfaceAttribs, 0
      );

      if (!EGL14.eglMakeCurrent(
            display,
            surface,
            surface,
            context)) {

        return "Unknown";
      }

      String vendor =
        GLES20.glGetString(GLES20.GL_VENDOR);

      String renderer =
        GLES20.glGetString(GLES20.GL_RENDERER);

      if (renderer == null) return "Unknown";

      renderer = renderer.trim();

      if (vendor != null) {
        vendor = vendor.trim();

        if (!renderer.startsWith(vendor)) {
          return vendor + " " + renderer;
        }
      }

      return renderer;

    } catch (Throwable ignored) {
      return "Unknown";

    } finally {

      if (display != EGL14.EGL_NO_DISPLAY) {

        EGL14.eglMakeCurrent(
          display,
          EGL14.EGL_NO_SURFACE,
          EGL14.EGL_NO_SURFACE,
          EGL14.EGL_NO_CONTEXT
        );

        if (surface != EGL14.EGL_NO_SURFACE) {
          EGL14.eglDestroySurface(
            display,
            surface
          );
        }

        if (context != EGL14.EGL_NO_CONTEXT) {
          EGL14.eglDestroyContext(
              display,
              context
          );
        }

        EGL14.eglTerminate(display);
      }
    }
  }

  private static String getResolution(Context ctx) {
    WindowManager wm =
      (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

    Display display = wm.getDefaultDisplay();
    Display.Mode mode = display.getMode();

    return String.format(
      "%dx%d @ %.0fHz",
      mode.getPhysicalHeight(),
      mode.getPhysicalWidth(),
      mode.getRefreshRate()
    );
  }

  private static String getSupportedAbi() {
    return String.join(", ", Build.SUPPORTED_ABIS);
  }

  private static String getCpuName() {
      String hardware = null;

      try (BufferedReader br =
           new BufferedReader(new FileReader("/proc/cpuinfo"))) {

          String line;

          while ((line = br.readLine()) != null) {

              if (line.startsWith("Hardware")) {
                  String[] s = line.split(":", 2);

                  if (s.length == 2) {
                      hardware = s[1].trim();
                      break;
                  }
              }
          }

      } catch (Exception ignored) {
      }

      return hardware != null ? hardware : Build.HARDWARE;
  }

  private static int getCpuCores() {
      File dir = new File("/sys/devices/system/cpu");

      File[] files = dir.listFiles(
          f -> f.getName().matches("cpu[0-9]+")
      );

      return files == null
          ? Runtime.getRuntime().availableProcessors()
          : files.length;
  }

  private static long getCpuFreq(int cpu) {
      try (BufferedReader br =
           new BufferedReader(
               new FileReader(
                   "/sys/devices/system/cpu/cpu"
                   + cpu
                   + "/cpufreq/cpuinfo_max_freq"
               ))) {

          return Long.parseLong(br.readLine());

      } catch (Exception ignored) {
          return 0;
      }
  }

  private static String getCpuInfo() {
    String name = getCpuName();
    int cores = getCpuCores();

    Map<Long, Integer> groups = new TreeMap<>(Collections.reverseOrder());

    long maxFreq = 0;

    for (int i = 0; i < cores; i++) {
      long freq = getCpuFreq(i);

      if (freq == 0)
        continue;

      groups.put(freq, groups.getOrDefault(freq, 0) + 1);

      if (freq > maxFreq)
        maxFreq = freq;
    }

    StringBuilder cluster = new StringBuilder();

    boolean first = true;

    for (int count : groups.values()) {
      if (!first)
        cluster.append("+");

      cluster.append(count);

      first = false;
    }

    double ghz = maxFreq / 1_000_000.0;

    if (cluster.length() == 0) {
      return String.format(
        "%s (%d cores)",
        name, cores
      );
    }

    return String.format(
      "%s (%s) @ %.2f GHz",
      name, cluster, ghz
    );
  }

  private static String getWM() {
    // simplify
    return "WindowManagerService (SurfaceFlinger)";
  }

  private static String formatGiB(long bytes) {
    return String.format("%.2f GiB", bytes / 1024.0 / 1024 / 1024);
  }

  private static String readFirstLine(String path) {
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      return br.readLine();
    } catch (Exception e) {
      return "Unknown";
    }
  }

  private static String getKernel() {
    String line = readFirstLine("/proc/version");
    if (!"Unknown".equals(line)) {
      return line;
    }

    return System.getProperty("os.version");
  }

  private static String getUptime() {
    long sec = SystemClock.elapsedRealtime() / 1000;

    long days = sec / 86400;
    sec %= 86400;

    long hours = sec / 3600;
    sec %= 3600;

    long mins = sec / 60;

    StringBuilder sb = new StringBuilder();

    if (days > 0)
      sb.append(days).append(" days, ");

    if (hours > 0)
      sb.append(hours).append(" hours, ");

    sb.append(mins).append(" mins");

    return sb.toString();
  }

  private static String getStorageInfo() {
    StatFs stat = new StatFs(
        Environment.getDataDirectory().getPath()
    );

    long totalStorage = stat.getTotalBytes();
    long freeStorage = stat.getAvailableBytes();
    long usedStorage = totalStorage - freeStorage;
    int storagePercent = (int) (usedStorage * 100 / totalStorage);
    return String.format(
          "%s / %s (%s)",
          formatGiB(totalStorage - freeStorage),
          formatGiB(totalStorage),
          percentColor(storagePercent)
        );
  }

  private static String getMemoryInfo() {

    long freeMem = 0;
    long totalMem = 0;

    try (BufferedReader br
        = new BufferedReader(new FileReader("/proc/meminfo"))) {

      String line;

      while ((line = br.readLine()) != null) {

        if (line.startsWith("MemAvailable:")) {
          String[] s = line.trim().split("\\s+");

          // kB -+> bytes
          freeMem = Long.parseLong(s[1]) * 1024;
        } else if (line.startsWith("MemTotal:")) {
          String[] s = line.trim().split("\\s+");

          // kB -+> bytes
          totalMem = Long.parseLong(s[1]) * 1024;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();

      return "Unknown";
    }

    long usedMem = totalMem - freeMem;
    int memPercent = (int) (usedMem * 100 / totalMem);
    return String.format(
      "%s / %s (%s)",
      formatGiB(usedMem),
      formatGiB(totalMem),
      percentColor(memPercent)
    );
  }

  private static String getSwapInfo() {
    long totalSwp = 0;
    long freeSwp = 0;

    try (BufferedReader br
        = new BufferedReader(new FileReader("/proc/meminfo"))) {

      String line;

      while ((line = br.readLine()) != null) {
        if (line.startsWith("SwapTotal:")) {
          String[] s = line.trim().split("\\s+");
          totalSwp = Long.parseLong(s[1]) * 1024;

        } else if (line.startsWith("SwapFree:")) {
          String[] s = line.trim().split("\\s+");
          freeSwp = Long.parseLong(s[1]) * 1024;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      return "Unknown";
    }

    if (totalSwp == 0) {
      return "Disabled";
    }

    long usedSwp = totalSwp - freeSwp;
    int swpPercent = (int) (usedSwp * 100 / totalSwp);

    return String.format(
      "%s / %s (%s)",
      formatGiB(usedSwp),
      formatGiB(totalSwp),
      percentColor(swpPercent)
    );
  }

  private static String getDensityName(int dpi) {
    if (dpi <= 120) return "ldpi";
    if (dpi <= 160) return "mdpi";
    if (dpi <= 240) return "hdpi";
    if (dpi <= 360) return "xhdpi";
    if (dpi <= 560) return "xxhdpi";

    return "xxxhdpi";
  }
  private static String getDpiInfo(Context ctx) {
    int dpi = ctx.getResources().getDisplayMetrics().densityDpi;

    return dpi + " dpi (" + getDensityName(dpi) + ")";
  }

  private static void prinHelp() {
    System.out.printf("""
Usage: %s [OPTIONS]

options:
  --help        print this help message
  --version     print afetch version
  --cfg         create a default config
  --no-logo     print info without the logo
\n""", PROGRAM_NAME);
  }

  private static void row(String key, Object value) {
    System.out.printf(
      GREEN_BOLD + "│ " +
      WHITE_BOLD + "%-11s" +
      RESET + " %s%n",
      key, value
    );
  }

  private static String getHost() {
    return String.format(
      "%s %s (%s)",
      Build.MANUFACTURER,
      Build.MODEL,
      Build.DEVICE
    );
  }

  private static String percentColor(int percent) {
    if (percent >= 80) {
      return RED_BOLD +
        String.valueOf(percent) + "%" + RESET;
    }

    if (percent >= 65) {
      return YELLOW_BOLD +
        String.valueOf(percent) + "%" + RESET;
    }

    return GREEN_BOLD +
      String.valueOf(percent) + "%" + RESET;
  }

  private static void printSystemInfo(Config afetchCfg)
      throws Exception {
    Context ctx = FakeContext.getSystemContext();

    if (afetchCfg.get(ConfigKey.LOGO)) {
      printLogo();
    }

    System.out.println(
      GREEN_BOLD + "╭───────────────────────────────╮" + RESET
    );

    row("OS",
      String.format(
        "%s (API %d)",
        getAndroidCodename(),
        Build.VERSION.SDK_INT
      )
    );

    if (afetchCfg.get(ConfigKey.HOST)) {
      row("Host", getHost());
    }

    if (afetchCfg.get(ConfigKey.BRAND)) {
      row("Brand", Build.BRAND);
    }

    if (afetchCfg.get(ConfigKey.RESOLUTION)) {
      row("Resolution", getResolution(ctx));
    }

    if (afetchCfg.get(ConfigKey.DPI)) {
      row("Dpi", getDpiInfo(ctx));
    }

    if (afetchCfg.get(ConfigKey.KERNEL)) {
      row("Kernel",
          "Linux " + System.getProperty("os.version"));
    }

    if (afetchCfg.get(ConfigKey.BATTERY)) {
      row("Battery", getBatteryInfo());
    }

    if (afetchCfg.get(ConfigKey.DE)) {
      // Fastfetch menyebutnya DE
      row("DE", Build.DISPLAY);
    }

    if (afetchCfg.get(ConfigKey.WM)) {
      row("WM", getWM());
    }

    if (afetchCfg.get(ConfigKey.CPU)) {
      row("CPU", getCpuInfo());
    }

    if (afetchCfg.get(ConfigKey.GPU)) {
      row("GPU", getGpuInfo());
    }

    if (afetchCfg.get(ConfigKey.ABI)) {
      row("ABI", getSupportedAbi());
    }

    if (afetchCfg.get(ConfigKey.UPTIME)) {
      row("Uptime", getUptime());
    }

    if (afetchCfg.get(ConfigKey.MEMORY)) {
      row("Memory", getMemoryInfo());
    }

    if (afetchCfg.get(ConfigKey.SWAP)) {
      row("Swap", getSwapInfo());
    }

    if (afetchCfg.get(ConfigKey.STORAGE)) {
      row("Storage", getStorageInfo());
    }

    if (afetchCfg.get(ConfigKey.LOCAL_IP)) {
      row("Local IP", getLocalIP());
    }

    if (afetchCfg.get(ConfigKey.APK_COUNT)) {
      row("Apk", getApkCount(ctx));
    }

    if (afetchCfg.get(ConfigKey.PACKAGE_COUNT)) {
      row("Package", getPackageInfo());
    }

    System.out.println(
      GREEN_BOLD + "╰───────────────────────────────╯" + RESET
    );
  }

  public static void main(String[] args) {

    if (Looper.getMainLooper() == null) {
      Looper.prepare();
    }

    try {

      Config afetchCfg = new Config();

      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("--cfg")) {
          afetchCfg.createDeafultConfig();
          System.exit(0);
        } else if (args[i].equals("--help")) {
          prinHelp();
          System.exit(0);
        } else if (args[i].equals("--no-logo")) {
          afetchCfg.set(ConfigKey.LOGO, false);
        } else if (args[i].equals("--version")) {
          System.out.println(VERSION);
          System.exit(0);
        }
      }

      printSystemInfo(afetchCfg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
