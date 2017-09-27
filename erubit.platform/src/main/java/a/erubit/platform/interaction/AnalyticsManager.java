package a.erubit.platform.interaction;

import android.support.v4.app.Fragment;
import java.util.Iterator;
import java.util.ServiceLoader;

public class AnalyticsManager {
    private static final IAnalyticsReporter mReporterInstance = Container.mInstance;
    private static final AnalyticsManager mInstance = new AnalyticsManager();

    public static AnalyticsManager i() {
        return mInstance;
    }

    private AnalyticsManager() {
        initialize();
    }

    public void initialize() {
        mReporterInstance.initialize();
    }

    public void reportFragmentChanged(Fragment fragment) {
        if (fragment != null)
            mReporterInstance.report(fragment.getClass().getSimpleName(), "fragment");
        else
            mReporterInstance.report("navigationFragment", "fragment");
    }

    public void reportInteractionScreen() {
        mReporterInstance.report("Interaction", "WindowManager");
    }


    private static class Container
    {
        static final IAnalyticsReporter mInstance;

        static
        {
            IAnalyticsReporter instance;
            Iterator<IAnalyticsReporter> loader = ServiceLoader.load(IAnalyticsReporter.class).iterator();

            if (loader.hasNext()) {
                instance = loader.next();
            }
            else {
                instance = new EmptyAnalyticsReporter();
            }

            mInstance = instance;
        }
    }


    private static class EmptyAnalyticsReporter implements IAnalyticsReporter {
        @Override
        public void initialize() { }
        @Override
        public void report(String event, String category) { }
    }
}
