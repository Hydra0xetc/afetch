package android.os;

import android.os.BatteryProperty;

/** @hide */
interface IBatteryPropertiesRegistrar {
    int getProperty(int id, inout BatteryProperty prop);
    void scheduleUpdate();
}
