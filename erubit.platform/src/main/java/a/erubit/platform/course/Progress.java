package a.erubit.platform.course;

import android.content.res.Resources;

import a.erubit.platform.android.App;
import a.erubit.platform.R;

public abstract class Progress {
    long nextInteractionDate = 0;
    public long interactionDate = 0;
    public long trainDate = 0;

    public int familiarity = 0;

    public int progress = 0;

    public String getExplanation() {
        Resources r = App.getContext().getResources();
        if (interactionDate == 0)
            return r.getString(R.string.unopened);

        return r.getString(
                R.string.progress_explanation,
                progress, familiarity);
    }
}
