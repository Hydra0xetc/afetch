package org.afetch;

import java.io.File;
import java.io.IOException;
import static org.afetch.Logger.Level.*;

public class Config {
  private String TAG = "Config";
  private Logger logger = Logger.getInstance();
  private String cfgDir = System.getenv("HOME") + "/.config/afetch";
  private String cfgPath = cfgDir + "/config.conf";

  public String getCfgPath() {
    return cfgPath;
  }

  public boolean exists() {
    return new File(cfgPath).exists();
  }

  public void createIfNotExists() {
    if (!exists()) {

      File dir = new File(cfgDir);
      File path = new File(cfgPath);
      try {
        if (!dir.exists() && !dir.mkdirs()) {
          logger.log(ERROR, TAG, "Failed to create config directory: " + cfgDir);
          return;
        }
        if (path.createNewFile()) {
          logger.log(INFO, TAG, "Created config at: " + cfgPath);
        }

      } catch (IOException e) {
        logger.log(ERROR, TAG, e.getMessage());
        e.printStackTrace();
      }
    }
  }

}
