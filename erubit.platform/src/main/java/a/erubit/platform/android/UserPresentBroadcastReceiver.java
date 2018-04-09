package a.erubit.platform.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class UserPresentBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        /*
         * Sent when the user is present after
         * device wakes up (e.g when the keyguard is gone)
         */
        if(Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            showFloatingView(context);
        }
    }

    private void showFloatingView(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(new Intent(context.getApplicationContext(), InteractionService.class));
        else
            context.startService(new Intent(context.getApplicationContext(), InteractionService.class));
    }
}
