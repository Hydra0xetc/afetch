package android.app;

import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;

/** @hide */
interface IActivityManager {
    Intent registerReceiver(
        in IApplicationThread caller,
        String callerPackage,
        in IIntentReceiver receiver,
        in IntentFilter filter,
        String requiredPermission,
        int userId,
        int flags
    );
}
