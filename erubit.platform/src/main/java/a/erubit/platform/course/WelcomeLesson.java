package a.erubit.platform.course;

import android.content.res.Resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import a.erubit.platform.android.App;
import a.erubit.platform.R;
import u.U;

public class WelcomeLesson extends Lesson {
    private String welcomeText;

    WelcomeLesson(Course course) {
        super(course);
    }

    @Override
    public PresentableDescriptor getNextPresentable() {
        String text = welcomeText +  App.getContext().getResources().getString(R.string.next_time_notice);
        return new PresentableDescriptor(text);
    }

    @Override
    public ArrayList<PresentableDescriptor> getPresentables() {
        ArrayList<PresentableDescriptor> descriptors = new ArrayList<>(1);
        descriptors.add(getNextPresentable());

        return descriptors;
    }

    private Progress loadProgress() {
        String json = ProgressManager.i().load(this.id);

        Progress progress = new Progress();

        if ("".equals(json) || null == json)
            return progress;

        JsonObject jo = new JsonParser().parse(json).getAsJsonObject();

        if (jo.has("interactionDate"))
            progress.interactionDate = jo.get("interactionDate").getAsLong();

        return progress;
    }

    @Override
    public a.erubit.platform.course.Progress updateProgress() {
        boolean isShown = mProgress.interactionDate > 0;

        mProgress.progress = mProgress.familiarity = isShown ? 100 : 0;

        return mProgress;
    }

    @Override
    public a.erubit.platform.course.Progress getProgress() {
        return updateProgress();
    }

    @Override
    public boolean hasInteraction() {
        return mProgress.interactionDate == 0;
    }

    WelcomeLesson loadFromResource(int resourceId) {
        try {
            String json = U.loadStringResource(resourceId);
            JSONObject jo = new JSONObject(json);

            id = jo.getString("id");
            name = U.getStringValue(jo, "title");
            welcomeText = U.getStringValue(jo, "description");
            mProgress = loadProgress();
        } catch (IOException | JSONException ignored) {
            ignored.printStackTrace();
            // TODO
        }

        return this;
    }

    private class Progress extends a.erubit.platform.course.Progress {
        @Override
        public String getExplanation() {
            Resources r = App.getContext().getResources();
            return r.getString(interactionDate == 0 ? R.string.unopened : R.string.finished);
        }
    }
}
