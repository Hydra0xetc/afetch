package org.afetch;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

import static org.afetch.Logger.Level.*;

public class Config {
  private static final String CFG_DIR = System.getenv("HOME") + "/.config/afetch";
  private static final String CFG_PATH = CFG_DIR + "/config.json";
  private final Map<String, Boolean> overrides = new HashMap<>();
  private JSONObject root;
  private Logger logger = Logger.getInstance();

  public Config() throws JSONException {
    if (new File(CFG_PATH).exists()) {
      load();
    } else {
      root = getDefaultConfig();
    }
  }

  private void load() {
    try {
      StringBuilder sb = new StringBuilder();

      try (BufferedReader reader = new BufferedReader(
            new FileReader(CFG_PATH)
            )) {
        String line;

        while ((line = reader.readLine()) != null) {
          sb.append(line).append('\n');
        }
      }

      root = new JSONObject(sb.toString());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public JSONArray getModules() throws JSONException {
    return root.getJSONArray("modules");
  }

  public void set(ConfigKey key, boolean value) {
    overrides.put(key.getKey(), value);
  }

  public boolean isEnabled(String type) {
    return overrides.getOrDefault(type, true);
  }

  public boolean get(ConfigKey key) {
    return isEnabled(key.getKey());
  }

  public String getCfgPath() {
    return CFG_PATH;
  }

  private static String row(String key, String value) {
    return String.format(
      "{green}│{reset} {white}%-11s{reset} %s",
      key,
      value
    );
  }

  public JSONObject getDefaultConfig() throws JSONException {
    JSONObject cfg = new JSONObject();
    JSONArray modules = new JSONArray();

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.LOGO.getKey())
      // TODO: add support ascii art file
      .put("format", "{green}{logo}{reset}")
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.HEADER.getKey())
      // TODO: auto reset color maybe??
      .put("format", "{green}╭───────────────────────────────╮{reset}")
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.OS.getKey())
      .put("format", row("OS", "{os} (API {api})"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.HOST.getKey())
      .put("format", row("Host", "{host}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.BRAND.getKey())
      .put("format", row("Brand", "{brand}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.RESOLUTION.getKey())
      .put("format", row("Resolution", "{resolution}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.DPI.getKey())
      .put("format", row("Dpi", "{dpi}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.KERNEL.getKey())
      .put("format", row("Kernel", "Linux {kernel}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.BATTERY.getKey())
      .put("format", row("Battery", "{battery}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.DE.getKey())
      .put("format", row("DE", "{de}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.WM.getKey())
      .put("format", row("WM", "{wm}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.CPU.getKey())
      .put("format", row("CPU", "{cpu}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.GPU.getKey())
      .put("format", row("GPU", "{gpu}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.ABI.getKey())
      .put("format", row("ABI", "{abi}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.UPTIME.getKey())
      .put("format", row("Uptime", "{uptime}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.MEMORY.getKey())
      .put("format", row("Memory", "{memory}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.SWAP.getKey())
      .put("format", row("Swap", "{swap}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.STORAGE.getKey())
      .put("format", row("Storage", "{storage}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.LOCAL_IP.getKey())
      .put("format", row("Local IP", "{localIP}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.APK_COUNT.getKey())
      .put("format", row("Apk", "{apkCount}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.PACKAGE_COUNT.getKey())
      .put("format", row("Package", "{packageCount}"))
    );

    modules.put(
      new JSONObject()
      .put("type", ConfigKey.FOOTER.getKey())
      .put("format", "{green}╰───────────────────────────────╯{reset}")
    );

    cfg.put("modules", modules);

    return cfg;
  }

  private void handleCreateDefaultConfig() throws JSONException {
      File cfgpath = new File(CFG_PATH);
      try {
        cfgpath.mkdirs();

        try (FileWriter writer = new FileWriter(CFG_PATH)) {
          writer.write(getDefaultConfig().toString(2));
        }

        logger.log(
          INFO, "afetch", "Creating new config at: " + CFG_PATH
        );

      } catch (IOException e) {
        throw new RuntimeException(e);
      }

  }

  public void createDefaultConfig() throws JSONException {
    File cfgpath = new File(CFG_DIR);

    if (cfgpath.exists()) {
      Scanner scanner = new Scanner(System.in);

      System.out.print("Config already exists, replace? (y/N): ");
      String input = scanner.nextLine().trim().toLowerCase();

      if (input.equals("Y") || input.equals("y")) {
        handleCreateDefaultConfig();
      } else {
        System.out.println("Canceling...");
        return;
      }
    } else {
      handleCreateDefaultConfig();
    }
  }
}
