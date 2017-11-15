package a.erubit.platform.android;

import android.app.Application;

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
    }
}