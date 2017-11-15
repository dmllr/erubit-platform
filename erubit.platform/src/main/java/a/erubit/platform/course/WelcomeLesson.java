package a.erubit.platform.course;

import android.content.Context;
import android.content.res.Resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import a.erubit.platform.R;
import u.U;

public class WelcomeLesson extends Lesson {
    private String welcomeText;

    WelcomeLesson(Course course) {
        super(course);
    }

    @Override
    public PresentableDescriptor getNextPresentable(Context context) {
        String text = welcomeText +  context.getString(R.string.next_time_notice);
        return new PresentableDescriptor(text);
    }

    @Override
    public ArrayList<PresentableDescriptor> getPresentables(Context context) {
        ArrayList<PresentableDescriptor> descriptors = new ArrayList<>(1);
        descriptors.add(getNextPresentable(context));

        return descriptors;
    }

    private Progress loadProgress(Context context) {
        String json = ProgressManager.i().load(context, this.id);

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
    public a.erubit.platform.course.Progress getProgress(Context context) {
        return updateProgress();
    }

    @Override
    public boolean hasInteraction() {
        return mProgress.interactionDate == 0;
    }

    WelcomeLesson loadFromResource(Context context, int resourceId) {
        try {
            String json = U.loadStringResource(context, resourceId);
            JSONObject jo = new JSONObject(json);

            id = jo.getString("id");
            name = U.getStringValue(context, jo, "title");
            welcomeText = U.getStringValue(context, jo, "description");
            mProgress = loadProgress(context);
        } catch (IOException | JSONException ignored) {
            ignored.printStackTrace();
            // TODO
        }

        return this;
    }

    private class Progress extends a.erubit.platform.course.Progress {
        @Override
        public String getExplanation(Context context) {
            Resources r = context.getResources();
            return r.getString(interactionDate == 0 ? R.string.unopened : R.string.finished);
        }
    }
}
