package a.erubit.platform.android;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import a.erubit.platform.course.CourseManager;
import a.erubit.platform.interaction.AnalyticsManager;
import a.erubit.platform.interaction.InteractionManager;

public class App extends Application {

    public App() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CourseManager.i().initialize(getApplicationContext());
        InteractionManager.i().initialize(getApplicationContext());
        AnalyticsManager.i().initialize(getApplicationContext());

        getApplicationContext().registerReceiver(
                new UserPresentBroadcastReceiver(),
                new IntentFilter(Intent.ACTION_USER_PRESENT));
    }
}