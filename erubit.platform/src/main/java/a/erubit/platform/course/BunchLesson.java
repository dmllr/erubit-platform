package a.erubit.platform.course;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import a.erubit.platform.R;
import a.erubit.platform.android.App;
import u.C;
import u.U;

public abstract class BunchLesson extends Lesson {
    private int contentResourceId;
    public ArrayList<Item> mSet;
    ArrayList<String> mVariants;

    private static final int TIME_TO_FORGET = 1000 * 60 * 60 * 24 * 2;  // Two days

    BunchLesson(Course course) {
        super(course);
    }

    protected abstract int getRankFamiliar();
    protected abstract int getRankLearned();
    protected abstract int getRankLearnedWell();

    @Override
    public PresentableDescriptor getNextPresentable() {
        loadHeavyContent();

        int size = mSet.size();

        if (size == 0)
            return PresentableDescriptor.ERROR;

        LinkedList<Item> availableItems = new LinkedList<>();
        for (int k = 0; k < size; k++) {
            Item c = mSet.get(k);
            Knowledge knowledge = c.getKnowledge();

            if (knowledge != Knowledge.Learned && knowledge != Knowledge.LearnedWell) {
                availableItems.add(c);
            }
        }

        size = availableItems.size();

        if (size == 0)
            return PresentableDescriptor.COURSE_LEARNED;

        Collections.sort(availableItems, (c1, c2) -> c1.showDate > c2.showDate ? -1 : (c1.showDate < c2.showDate ? 1 : 0));

        size = availableItems.size();

        if (size == 0)
            return PresentableDescriptor.ERROR;

        if (size > 2) {
            availableItems.removeFirst();
            availableItems.removeFirst();
            size -= 2;
        }

        int problemIndex = new Random().nextInt(size);
        Item problemItem = availableItems.get(problemIndex);

        return getPresentable(problemItem);
    }

    @NonNull
    protected abstract PresentableDescriptor getPresentable(Item problemItem);

    @Override
    public ArrayList<PresentableDescriptor> getPresentables() {
        loadHeavyContent();

        int size = mSet.size();
        ArrayList<PresentableDescriptor> descriptors = new ArrayList<>(size);

        for (Item item: mSet) {
            PresentableDescriptor descriptor = getPresentable(item);
            descriptors.add(descriptor);
        }

        return descriptors;
    }

    public void loadHeavyContent() {
        loadFromResource(contentResourceId, true);
    }

    @Override
    public a.erubit.platform.course.Progress updateProgress() {
        int size = mSet.size();

        Progress progress = new Progress(size);

        boolean hasUnknown = false;
        boolean hasSomething = false;
        for (int k = 0; k < size; k++) {
            Item c = mSet.get(k);
            Knowledge knowledge = c.getKnowledge();

            //noinspection ConstantConditions
            hasUnknown = hasUnknown || (knowledge != Knowledge.Learned && knowledge != Knowledge.LearnedWell);
            hasSomething = hasSomething || (knowledge != Knowledge.LearnedWell);

            if (hasUnknown)
                break;
        }

        progress.nextInteractionDate = -1;
        if (hasUnknown)
            progress.nextInteractionDate = 0;
        else if (hasSomething)
            progress.nextInteractionDate = System.currentTimeMillis() + TIME_TO_FORGET;

        progress.interactionDate = mProgress.interactionDate;
        progress.trainDate = mProgress.trainDate;

        for (int k = 0; k < size; k++) {
            Item c = mSet.get(k);
            ItemProgress cs = new ItemProgress();
            cs.showDate = c.showDate;
            cs.failDate = c.failDate;
            cs.raiseDate = c.raiseDate;
            cs.touchDate = c.touchDate;
            cs.knowledgeLevel = c.knowledgeLevel;
            progress.append(c.id, cs);
        }

        if (size == 0) {
            progress.familiarity = 0;
            progress.progress = 0;
            return progress;
        }

        int t = 0, r = 0;
        size = progress.map.size();
        for (int k = 0; k < size; k++) {
            ItemProgress cs = progress.map.get(progress.map.keyAt(k));
            if (cs.touchDate > 0)
                t++;
            if (cs.knowledgeLevel == getRankFamiliar())
                r++;
        }
        progress.familiarity = 100 * t / size;
        progress.progress = 100 * r / size;

        return mProgress = progress;
    }

    @Override
    public a.erubit.platform.course.Progress getProgress() {
        mProgress = loadProgress(true);
        return mProgress;
    }

    private Progress loadProgress(boolean withHeavyContent) {
        String json = ProgressManager.i().load(this.id);

        Progress progress = new Progress();

        if ("".equals(json) || null == json)
            return progress;

        JsonObject jo = new JsonParser().parse(json).getAsJsonObject();

        if (jo.has("nextInteractionDate"))
            progress.nextInteractionDate = jo.get("nextInteractionDate").getAsLong();
        if (jo.has("interactionDate"))
            progress.interactionDate = jo.get("interactionDate").getAsLong();
        if (jo.has("trainDate"))
            progress.trainDate = jo.get("trainDate").getAsLong();
        if (jo.has("progress"))
            progress.progress = jo.get("progress").getAsInt();
        if (jo.has("familiarity"))
            progress.familiarity = jo.get("familiarity").getAsInt();

        if (withHeavyContent && jo.has("map")) {
            jo = jo.get("map").getAsJsonObject();

            int size = jo.get("mSize").getAsInt();

            JsonArray keys = jo.get("mKeys").getAsJsonArray();
            JsonArray values = jo.get("mValues").getAsJsonArray();
            for (int k = 0; k < size; k++) {
                progress.appendFromJson(keys.get(k).getAsInt(), values.get(k).getAsJsonObject());
            }
        }

        return progress;
    }

    @Override
    public boolean hasInteraction() {
        return mProgress.nextInteractionDate >= 0 && mProgress.nextInteractionDate <= System.currentTimeMillis();
    }

    BunchLesson loadFromResource(int resourceId, boolean withHeavyContent) {
        this.contentResourceId = resourceId;
        try {
            String json = U.loadStringResource(resourceId);
            JSONObject jo = new JSONObject(json);

            id = jo.getString("id");
            name = U.getStringValue(jo, "title");

            Progress progress = loadProgress(withHeavyContent);

            if (withHeavyContent) {
                mSet = new ArrayList<>();
                JSONArray jset = jo.getJSONArray("set");
                for (int i = 0; i < jset.length(); i++) {
                    JSONObject jso = jset.getJSONObject(i);
                    mSet.add(new Item().fromJsonObject(jso).withProgress(progress));
                }

                mVariants = new ArrayList<>(20);
                JSONArray jvar = jo.getJSONArray("variants");
                for (int i = 0; i < jvar.length(); i++) {
                    mVariants.add(jvar.getString(i));
                }
            }
            this.mProgress = progress;
        } catch (IOException | JSONException ignored) {
            ignored.printStackTrace();
            // TODO
        }

        return this;
    }

    public class Problem extends Lesson.Problem {
        public final Item item;
        public String[] variants;

        Problem(Lesson lesson, Item item) {
            super(lesson);
            this.item = item;
            text = item.character;
            meaning = item.meaning;
            variants = new String[C.NUMBER_OF_ANSWERS];
        }

        @Override
        public void spied() {
            item.fail();
        }

        @Override
        public void treatResult() {
            if (mSucceed)
                item.raise();
            else
                item.fail();

            item.showDate = System.currentTimeMillis();
        }

        public Knowledge getKnowledge() {
            return item.getKnowledge();
        }
    }

    public class Item {
        int id;
        public String character;
        String meaning;

        public int knowledgeLevel = 0;
        long touchDate = 0;
        long raiseDate = 0;
        long failDate = 0;
        long showDate = 0;

        Item() {
        }

        Item fromJsonObject(JSONObject jso) throws JSONException {
            this.id = jso.getInt("i");

            this.character = jso.getString("c");
            this.meaning = jso.getString("m");

            return this;
        }

        Item withProgress(Progress progress) {
            ItemProgress storage = progress.get(this.id);

            this.knowledgeLevel = storage.knowledgeLevel;
            this.touchDate = storage.touchDate;
            this.raiseDate = storage.raiseDate;
            this.failDate = storage.failDate;
            this.showDate = storage.showDate;

            return this;
        }

        Knowledge getKnowledge() {
            if (knowledgeLevel >= getRankLearnedWell())
                return  Knowledge.LearnedWell;
            else if (knowledgeLevel >= getRankLearned() && knowledgeLevel < getRankLearnedWell()) {
                if (System.currentTimeMillis() > raiseDate + TIME_TO_FORGET)
                    return Knowledge.PossibleForgotten;
                else
                    return Knowledge.Learned;
            }
            else if (knowledgeLevel >= getRankFamiliar() && knowledgeLevel < getRankLearned())
                return Knowledge.Familiar;
            else if (knowledgeLevel < getRankFamiliar()) {
                if (touchDate == 0)
                    return Knowledge.Untouched;
                else
                    return Knowledge.Unknown;
            }
            else
                return Knowledge.Unknown;
        }

        void raise() {
            if (knowledgeLevel < getRankLearnedWell())
                knowledgeLevel++;
            touchDate = raiseDate = System.currentTimeMillis();
        }

        void fail() {
            knowledgeLevel = 0;
            touchDate = failDate = System.currentTimeMillis();
        }
    }

    private class Progress extends a.erubit.platform.course.Progress {
        final SparseArray<ItemProgress> map;

        private final ItemProgress defaultValue = new ItemProgress();

        Progress() {
            map = new SparseArray<>();
        }

        Progress(int size) {
            map = new SparseArray<>(size);
        }

        void append(int id, ItemProgress cs) {
            map.append(id, cs);
        }

        void appendFromJson(int id, JsonObject jo) {
            ItemProgress cs = new ItemProgress();
            if (jo.has("knowledgeLevel"))
                cs.knowledgeLevel = jo.get("knowledgeLevel").getAsInt();
            if (jo.has("touchDate"))
                cs.touchDate = jo.get("touchDate").getAsLong();
            if (jo.has("raiseDate"))
                cs.raiseDate = jo.get("raiseDate").getAsLong();
            if (jo.has("failDate"))
                cs.failDate = jo.get("failDate").getAsLong();
            if (jo.has("showDate"))
                cs.showDate = jo.get("showDate").getAsLong();

            append(id, cs);
        }

        ItemProgress get(int id) {
            return map.get(id, defaultValue);
        }

        @Override
        public String getExplanation() {
            int t = 0, f = 0, l = 0, lw = 0;
            int size = map.size();
            for (int k = 0; k < size; k++) {
                ItemProgress cs = map.get(map.keyAt(k));
                if (cs.touchDate > 0)
                    t++;
                if (cs.knowledgeLevel == getRankFamiliar())
                    f++;
                if (cs.knowledgeLevel == getRankLearned())
                    l++;
                if (cs.knowledgeLevel == getRankLearnedWell())
                    lw++;
            }
            t = Math.max(0, t - l - lw);

            Resources r = App.getContext().getResources();
            if (t + f + l + lw == 0)
                return r.getString(R.string.unopened);
            if (lw == size)
                return r.getString(R.string.finished);

            return r.getString(R.string.set_lesson_progress_explanation, t, f, l);
        }
    }

    public enum Knowledge {
        Untouched,
        Unknown,
        Familiar,
        PossibleForgotten,
        Learned,
        LearnedWell
    }
}
