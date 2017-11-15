package a.erubit.platform.course;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import u.U;

public abstract class Course {
    public String id;
    public String name;
    public String description;
    public boolean defaultActive;
    ArrayList<String> lessonResources;

    private ArrayList<Lesson> mLessons;

    public abstract Progress getProgress();

    public ArrayList<Lesson> getLessons(Context context) {
        if (mLessons != null)
            return mLessons;

        if (lessonResources == null)
            return new ArrayList<>(0);

        int size = lessonResources.size();
        String packageName = context.getPackageName();
        ArrayList<Lesson> lessons = new ArrayList<>(size);
        for (int k = 0; k < size; k++) {
            int resourceId = context.getResources().getIdentifier(lessonResources.get(k), "raw", packageName);

            try {
                JSONObject jo = new JSONObject(U.loadStringResource(context, resourceId));
                String type = jo.getString("type");
                switch (type) {
                    case "Welcome":
                        lessons.add(new WelcomeLesson(this).loadFromResource(context, resourceId));
                        break;
                    case "Set":
                        lessons.add(new SetLesson(this).loadFromResource(context, resourceId, false));
                        break;
                    case "Phrase":
                        lessons.add(new PhraseLesson(this).loadFromResource(context, resourceId, false));
                        break;
                    case "Character":
                        lessons.add(new CharacterLesson(this).loadFromResource(context, resourceId, false));
                        break;
                    case "Vocabulary":
                        lessons.add(new VocabularyLesson(this).loadFromResource(context, resourceId, false));
                        break;
                }
            } catch (JSONException | IOException ignored) {
            }
        }

        return mLessons = lessons;
    }

    public Lesson getLesson(String id) {
        for (Lesson lesson: mLessons)
            if (id.equals(lesson.id))
                return lesson;
        return null;
    }
}
