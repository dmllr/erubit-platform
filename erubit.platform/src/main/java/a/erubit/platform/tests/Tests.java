package a.erubit.platform.tests;

import android.content.Context;

import a.erubit.platform.course.CourseManager;

public class Tests {
    public static void courseManagerInitialize(Context context) {
        CourseManager.i().initialize(context);
    }
}
