package org.afetch;

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

import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.json.JSONObject;

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
  private static String prefix = System.getenv("PREFIX");

  private static void printLogo() {
    final String RESET = "\u001B[0m";
    final String BODY  = "\u001B[38;2;52;168;83m";
    final String TEXT  = "\u001B[38;2;61;220;132m";

    System.out.println(BODY + """
   ;,                    ,;
    ';,.--------------.,;'
    ,'                  ',
  ,'                      ',
 /      O           O       \\
|                            |
|                            |
'----------------------------'
""" + TEXT + """
 ******    ANDROID     ******
""" + RESET);
  }
  private static String getPackageInfo() {
    StringBuilder result = new StringBuilder();

    // There are only two package managers in Termux, pacman and apt/dpkg
    int pacmanCount = countPacman(prefix);
    if (pacmanCount >= 0) {
      appendResult(result, pacmanCount, "pacman");
    }

    int dpkgCount = countDpkg(prefix);
    if (dpkgCount >= 0) {
      appendResult(result, dpkgCount, "dpkg");
    }

    return result.length() > 0 ? result.toString() : "Unknown";
  }

  private static void appendResult(StringBuilder sb, int count, String name) {
    if (sb.length() > 0) sb.append(", ");
    sb.append(count).append(" (").append(name).append(")");
  }

  private static int countPacman(String prefix) {
    try {
      String basePath = prefix + "/var/lib/pacman/local";
      File dir = new File(basePath);
      File[] entries = dir.listFiles(File::isDirectory);
      return entries != null ? entries.length : -1;
    } catch (Exception e) {
      return -1;
    }
  }

  private static int countDpkg(String prefix) {
    try {
      String basePath = prefix + "/var/lib/dpkg/status";
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

  /* I Already Try Using BatteryManager with FakeContext (System Context)
   * And I Got The Following Error.
   *
   * ```console
   * java.lang.SecurityException: Unable to find app for caller
   * android.app.IApplicationThread$Stub$Proxy@d0d0873 (pid=20520) when
   * registering receiver null
   * ```
   * For Now Lets Use `termux-battery-status`
   */
    private static String getBatteryInfo() {
      // cheat a little bit :v, but slow :(
    String exe = prefix + "/bin/termux-battery-status";
    if (new File(exe).exists()) {
      try {
        Process p = Runtime.getRuntime().exec(
          exe
        );

        BufferedReader br = new BufferedReader(
          new InputStreamReader(p.getInputStream())
        );

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
          sb.append(line);
        }

        JSONObject obj = new JSONObject(sb.toString());

        return String.format(
          "%d%% (%s, %.1f°C)",
          obj.getInt("percentage"),
          obj.getString("status")
          .toLowerCase(),
          obj.getDouble("temperature")
        );

      } catch (Exception e) {
        e.printStackTrace();
        return "Unknown";
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
      "%dx%d@%.0fHz",
      mode.getPhysicalWidth(),
      mode.getPhysicalHeight(),
      mode.getRefreshRate()
    );
  }

  private static String getCpuArch() {
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

  private static long getTotalRam() {
    try (BufferedReader br =
        new BufferedReader(new FileReader("/proc/meminfo"))) {

      String line;

      while ((line = br.readLine()) != null) {

        if (line.startsWith("MemTotal:")) {
          String[] s = line.trim().split("\\s+");

          // kB -+> bytes
          return Long.parseLong(s[1]) * 1024;
        }
      }

    } catch (Exception ignored) {
    }

    return 0;
  }

  private static long getAvailableRam() {
    try (BufferedReader br
        = new BufferedReader(new FileReader("/proc/meminfo"))) {

      String line;

      while ((line = br.readLine()) != null) {

        if (line.startsWith("MemAvailable:")) {
          String[] s = line.trim().split("\\s+");

          // kB -+> bytes
          return Long.parseLong(s[1]) * 1024;
        }
      }

    } catch (Exception ignored) {
    }

    return 0;
  }

  public static void main(String[] args) {

    if (Looper.getMainLooper() == null) {
      Looper.prepare();
    }

    try {

      Config afetchCfg = new Config();
      // afetchCfg.createDeafultConfig();

      Context ctx = FakeContext.getSystemContext();

      // RAM
      long totalRam = getTotalRam();
      long freeRam = getAvailableRam();
      long usedRam = totalRam - freeRam;

      // Storage
      StatFs stat = new StatFs(Environment.getDataDirectory().getPath());

      long totalStorage = stat.getTotalBytes();
      long freeStorage = stat.getAvailableBytes();

      printLogo();
      System.out.println("╭───────────────────────────────╮");
      System.out.printf("│ OS          %s (API %d)%n",
          getAndroidCodename(), Build.VERSION.SDK_INT);
      if (afetchCfg.get(ConfigKey.RESOLUTION)) {
        System.out.printf("│ Resolution  %s%n", getResolution(ctx));
      }

      if (afetchCfg.get(ConfigKey.KERNEL)) {
        System.out.printf("│ Kernel      Linux %s%n",
            System.getProperty("os.version"));
      }

      if (afetchCfg.get(ConfigKey.BATTERY)) {
        System.out.printf("│ Battery     %s%n", getBatteryInfo());
      }

      if (afetchCfg.get(ConfigKey.BRAND)) {
        System.out.printf("│ Brand       %s%n", Build.BRAND);
      }

      if (afetchCfg.get(ConfigKey.VENDOR)) {
        System.out.printf("│ Vendor      %s%n", Build.MANUFACTURER);
      }

      if (afetchCfg.get(ConfigKey.DE)) {
        // In fastfetch this thing called DE :V
        System.out.printf("│ DE          %s%n", Build.DISPLAY);
      }

      if (afetchCfg.get(ConfigKey.WM)) {
        System.out.printf("│ WM          %s%n", getWM());
      }

      if (afetchCfg.get(ConfigKey.CPU)) {
        System.out.printf("│ CPU         %s%n", getCpuInfo());
      }

      if (afetchCfg.get(ConfigKey.GPU)) {
        System.out.printf("│ GPU         %s%n", getGpuInfo());
      }

      if (afetchCfg.get(ConfigKey.CPU_ARCH)) {
        System.out.printf("│ CPUArch     %s%n", getCpuArch());
      }

      if (afetchCfg.get(ConfigKey.UPTIME)) {
        System.out.printf("│ Uptime      %s%n", getUptime());
      }

      if (afetchCfg.get(ConfigKey.MEMORY)) {
        System.out.printf("│ Memory      %s / %s%n",
            formatGiB(usedRam), formatGiB(totalRam));
      }

      if (afetchCfg.get(ConfigKey.STORAGE)) {
        System.out.printf("│ Storage     %s / %s%n",
          formatGiB(totalStorage - freeStorage),
          formatGiB(totalStorage));
      }

      if (afetchCfg.get(ConfigKey.APK_COUNT)) {
        System.out.printf("│ Apk         %s%n", getApkCount(ctx));
      }

      if (afetchCfg.get(ConfigKey.PACKAGE_COUNT)) {
        System.out.printf("│ Package     %s%n", getPackageInfo());
      }

      System.out.println("╰───────────────────────────────╯");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
