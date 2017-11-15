package a.erubit.platform.interaction;

import android.content.Context;

public interface IAnalyticsReporter {
    void initialize(Context context);

    void report(String event, String category);
}
