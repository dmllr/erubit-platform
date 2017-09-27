package a.erubit.platform.course;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import u.U;

public class LanguageCourse extends Course {
    @Override
    public Progress getProgress() {
        return new Progress() {
        };
    }

    public void loadFromResource(int resourceId) {
        try {
            String json = U.loadStringResource(resourceId);
            loadFromString(json);
        } catch (IOException ignored) {
            ignored.printStackTrace();
            // TODO
        }
    }

    private void loadFromString(String json) {
        try {
            JSONObject jo = new JSONObject(json);

            id = jo.getString("id");
            name = U.getStringValue(jo, "title");
            description = U.getStringValue(jo, "description");

            if (jo.has("lessons")) {
                JSONArray jd = jo.getJSONArray("lessons");
                int size = jd.length();
                lessonResources = new ArrayList<>(size);
                for (int k = 0; k < size; k++)
                    lessonResources.add(jd.getString(k));
            }

            Progress progress = getProgress();

        } catch (JSONException ignored) {
            ignored.printStackTrace();
            // TODO
        }
    }
}
