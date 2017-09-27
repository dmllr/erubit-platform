package a.erubit.platform.interaction;

public interface IAnalyticsReporter {
    void initialize();

    void report(String event, String category);
}
