package android.app;

import android.os.IBinder;
import android.os.ServiceManager;

public class ActivityManager {

  public static IActivityManager getService() {
    IBinder b = ServiceManager.getService("activity");
    return IActivityManager.Stub.asInterface(b);
  }
}
