package a.erubit.platform.course;

import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import a.erubit.platform.android.App;
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

    public void initialize() {
        String packageName = App.getContext().getPackageName();
        int resourceId = App.getContext().getResources().getIdentifier("_contents", "raw", packageName);
        if (resourceId != 0) {
            try {
                String json = U.loadStringResource(resourceId);
                JSONArray ja = new JSONArray(json);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    resourceId = App.getContext().getResources().getIdentifier(jo.getString("name"), "raw", packageName);

                    Course c1 = fromResourceId(resourceId);
                    c1.defaultActive = jo.getBoolean("active");
                    mCourses.add(c1);
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }

        if (!CourseManager.i().hasSavedSettings()) {
            for (Course course: mCourses)
                if (course.defaultActive)
                    CourseManager.i().setActive(course);
        }
        updateActivity();
    }

    private LanguageCourse fromResourceId(int resourceId) {
        LanguageCourse c = new LanguageCourse();
        c.loadFromResource(resourceId);
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

    public void setActive(Course course) {
        if (!mActiveCourses.contains(course))
            mActiveCourses.add(course);

        save();
    }

    public void setInactive(Course course) {
        if (mActiveCourses.contains(course))
            mActiveCourses.remove(course);

        save();
    }

    private void save() {
        int size = mActiveCourses.size();
        ArrayList<String> list = new ArrayList<>(size);
        for (int k = 0; k < size; k++)
            list.add(mActiveCourses.get(k).id);

        new TinyDB(App.getContext()).putListString(C.SP_ACTIVE_COURSES, list);
    }

    private boolean hasSavedSettings() {
        return new TinyDB(App.getContext()).getListString(C.SP_ACTIVE_COURSES).size() > 0;
    }

    private void updateActivity() {
        ArrayList<String> list = new TinyDB(App.getContext()).getListString(C.SP_ACTIVE_COURSES);

        //noinspection unchecked
        mActiveCourses = (ArrayList<Course>) mCourses.clone();
        for (int k = 0; k < mActiveCourses.size(); k++)
            if (!list.contains(mActiveCourses.get(k).id)) {
                mActiveCourses.remove(k);
                k--;
            }
    }

    public Lesson getNextLesson() {
        int size = mActiveCourses.size();
        if (size < 1)
            return null;

        int i = new Random().nextInt(mActiveCourses.size());
        Course course = mActiveCourses.get(i);

        return getNextLesson(course);
    }

    public Lesson getNextLesson(Course course) {
        ArrayList<Lesson> lessons = course.getLessons();

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

    public String getSharingText() {
        Resources Rs = App.getContext().getResources();
        String r = "I'm learning now ";
        int size = mActiveCourses.size();
        if (size == 0)
            r += Rs.getString(R.string.app_name) + " courses";
        else {
            for (int i = 0; i < size - 1; i++) {
                r += mActiveCourses.get(i).name;
                if (i < size - 2)
                    r += ", ";
            }
            if (size > 1)
                r += " and ";
            r += mActiveCourses.get(size - 1).name;
            r += " of " + Rs.getString(R.string.app_name) + " courses";
        }
        r += ".\nJoin me, download Android app at" + " " + Rs.getString(R.string.play_url, App.getContext().getPackageName());
        return r;
    }
}
