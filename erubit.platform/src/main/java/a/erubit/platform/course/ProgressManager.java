package a.erubit.platform.course;

import android.content.Context;

import com.google.gson.Gson;

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

    public void save(Context context,Lesson lesson) {
        save(context, lesson.id, lesson.updateProgress());
    }

    private void save(Context context, String id, Progress progress) {
        String json = new Gson().toJson(progress);
        cacheMap.put(id, json);
        new TinyDB(context.getApplicationContext()).putString(C.PROGRESS_KEY + id, json);
    }

    String load(Context context, String id) {
        String cached = cacheMap.get(id);
        if (null != cached)
            return cached;

        return new TinyDB(context.getApplicationContext()).getString(C.PROGRESS_KEY + id);
    }
}
