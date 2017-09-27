package a.erubit.platform.course;

import com.google.gson.Gson;

import a.erubit.platform.android.App;
import t.SelfExpiringHashMap;
import t.SelfExpiringMap;
import t.TinyDB;
import u.C;

public class ProgressManager {
    private static final ProgressManager ourInstance = new ProgressManager();

    public static ProgressManager i() {
        return ourInstance;
    }

    private final SelfExpiringMap<String, String> cacheMap = new SelfExpiringHashMap<>(1000, 10);

    public void save(Lesson lesson) {
        save(lesson.id, lesson.updateProgress());
    }

    private void save(String id, Progress progress) {
        String json = new Gson().toJson(progress);
        cacheMap.put(id, json);
        new TinyDB(App.getContext()).putString(C.PROGRESS_KEY + id, json);
    }

    String load(String id) {
        String cached = cacheMap.get(id);
        if (null != cached)
            return cached;

        return new TinyDB(App.getContext()).getString(C.PROGRESS_KEY + id);
    }
}
