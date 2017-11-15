package a.erubit.platform.course;

import android.content.Context;
import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import a.erubit.platform.R;
import t.TinyDB;
import u.C;
import u.U;

public class CourseManager {
    private static final CourseManager ourInstance = new CourseManager();
    private final ArrayList<Course> mCourses = new ArrayList<>();
    private ArrayList<Course> mActiveCourses = new ArrayList<>();

    public static CourseManager i() {
        return ourInstance;
    }

    private CourseManager() {
    }

    public void initialize(Context context) {
        String packageName = context.getPackageName();
        int resourceId = context.getResources().getIdentifier("_contents", "raw", packageName);
        if (resourceId != 0) {
            try {
                String json = U.loadStringResource(context, resourceId);
                JSONArray ja = new JSONArray(json);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    resourceId = context.getResources().getIdentifier(jo.getString("name"), "raw", packageName);

                    Course c1 = fromResourceId(context, resourceId);
                    c1.defaultActive = jo.getBoolean("active");
                    mCourses.add(c1);
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }

        if (!CourseManager.i().hasSavedSettings(context)) {
            for (Course course: mCourses)
                if (course.defaultActive)
                    CourseManager.i().setActive(context, course);
        }
        updateActivity(context);
    }

    private LanguageCourse fromResourceId(Context context, int resourceId) {
        LanguageCourse c = new LanguageCourse();
        c.loadFromResource(context, resourceId);
        return c;
    }
    public ArrayList<Course> getCourses() {
        return mCourses;
    }

    public Course getCourse(String id) {
        for (Course c: mCourses)
            if (c.id.equals(id))
                return c;
        return null;
    }

    public boolean isActive(Course course) {
        return mActiveCourses.contains(course);
    }

    public void setActive(Context context, Course course) {
        if (!mActiveCourses.contains(course))
            mActiveCourses.add(course);

        save(context);
    }

    public void setInactive(Context context, Course course) {
        if (mActiveCourses.contains(course))
            mActiveCourses.remove(course);

        save(context);
    }

    private void save(Context context) {
        int size = mActiveCourses.size();
        ArrayList<String> list = new ArrayList<>(size);
        for (int k = 0; k < size; k++)
            list.add(mActiveCourses.get(k).id);

        new TinyDB(context.getApplicationContext()).putListString(C.SP_ACTIVE_COURSES, list);
    }

    private boolean hasSavedSettings(Context context) {
        return new TinyDB(context.getApplicationContext()).getListString(C.SP_ACTIVE_COURSES).size() > 0;
    }

    private void updateActivity(Context context) {
        ArrayList<String> list = new TinyDB(context.getApplicationContext()).getListString(C.SP_ACTIVE_COURSES);

        //noinspection unchecked
        mActiveCourses = (ArrayList<Course>) mCourses.clone();
        for (int k = 0; k < mActiveCourses.size(); k++)
            if (!list.contains(mActiveCourses.get(k).id)) {
                mActiveCourses.remove(k);
                k--;
            }
    }

    public Lesson getNextLesson(Context context) {
        int size = mActiveCourses.size();
        if (size < 1)
            return null;

        int i = new Random().nextInt(mActiveCourses.size());
        Course course = mActiveCourses.get(i);

        return getNextLesson(context, course);
    }

    public Lesson getNextLesson(Context context, Course course) {
        ArrayList<Lesson> lessons = course.getLessons(context);

        int size = lessons.size();
        if (size < 1)
            return null;

        for (int k = 0; k < size; k++) {
            Lesson l = lessons.get(k);
            if (l.hasInteraction())
                return l;
        }

        return null;
    }

    public String getSharingText(Context context) {
        Resources resources = context.getResources();
        StringBuilder r = new StringBuilder("I'm learning now ");
        int size = mActiveCourses.size();
        if (size == 0)
            r.append(resources.getString(R.string.app_name)).append(" courses");
        else {
            for (int i = 0; i < size - 1; i++) {
                r.append(mActiveCourses.get(i).name);
                if (i < size - 2)
                    r.append(", ");
            }
            if (size > 1)
                r.append(" and ");
            r.append(mActiveCourses.get(size - 1).name);
            r.append(" of ").append(resources.getString(R.string.app_name)).append(" courses");
        }
        r.append(".\nJoin me, download Android app at" + " ").append(resources.getString(R.string.play_url, context.getPackageName()));

        return r.toString();
    }
}
