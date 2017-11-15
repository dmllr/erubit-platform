package a.erubit.platform.android;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import a.erubit.platform.interaction.AnalyticsManager;
import a.erubit.platform.interaction.InteractionManager;
import a.erubit.platform.course.CourseManager;

public class App extends Application {
    public App() {
        super();
    }

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;

        CourseManager.i().initialize();
        InteractionManager.i().initialize();
        AnalyticsManager.i().initialize();
    }

    public static Context getContext() {
        return mContext;
    }
}