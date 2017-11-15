package a.erubit.platform.android;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import a.erubit.platform.BuildConfig;
import a.erubit.platform.interaction.AnalyticsManager;
import a.erubit.platform.interaction.InteractionManager;
import a.erubit.platform.R;
import t.TinyDB;
import u.C;


public class InteractionService extends Service implements InteractionManager.InteractionListener {

    private WindowManager mWindowManager;
    private View mInteractionView;
    private long mNextTimeSlot = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean enabled = new TinyDB(getApplicationContext()).getBoolean(C.SP_ENABLED_ON_UNLOCK, true)
                && System.currentTimeMillis() > mNextTimeSlot
                && (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(getApplicationContext()));

        if(enabled && (mInteractionView == null || mInteractionView.getWindowToken() == null)) {
            mInteractionView = InteractionManager.i().getInteractionView(getApplicationContext(), this);

            if (mInteractionView != null) {
                mInteractionView.findViewById(R.id.quickButtonBar).setVisibility(View.VISIBLE);
                mWindowManager.addView(mInteractionView, mInteractionView.getLayoutParams());
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // only if interaction window is shown now
        if (mInteractionView != null && mInteractionView.getWindowToken() != null) {
            InteractionManager.i().onConfigurationChanged(getApplicationContext(), newConfig);
            updateView();
        }
    }

    private void updateView() {
        removeView();

        mInteractionView = InteractionManager.i().getLastInteractionView(getApplicationContext(), this);

        if (mInteractionView != null) {
            mInteractionView.findViewById(R.id.quickButtonBar).setVisibility(View.VISIBLE);
            mWindowManager.addView(mInteractionView, mInteractionView.getLayoutParams());

            AnalyticsManager.i().reportInteractionScreen();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mInteractionView = null;
    }

    private void removeView() {
        if (mInteractionView != null && mInteractionView.getWindowToken() != null) {
            mWindowManager.removeView(mInteractionView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeView();
    }

    @Override
    public void onInteraction(InteractionManager.InteractionEvent event) {
        switch (event) {
            case BUSY1:
                mNextTimeSlot = System.currentTimeMillis() + C.TIME_1H;
                break;
            case BUSY2:
                mNextTimeSlot = System.currentTimeMillis() + C.TIME_2H;
                break;
            case BUSY4:
                mNextTimeSlot = System.currentTimeMillis() + C.TIME_4H;
                break;
        }
        if (event != InteractionManager.InteractionEvent.NEGATIVE)
            removeView();
    }
}