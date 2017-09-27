package a.erubit.platform.android;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import a.erubit.platform.interaction.InteractionManager;
import a.erubit.platform.R;

public abstract class TrainingFragment extends Fragment implements InteractionManager.InteractionListener, IUxController {

    private ViewAnimator mViewAnimator;
    private OnTrainingInteractionListener mListener;

    public TrainingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);
        mViewAnimator = view.findViewById(R.id.viewAnimator);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        InteractionManager.i().onConfigurationChanged(newConfig);
        updateView();
    }

    private void updateView() {
        View v = InteractionManager.i().getLastInteractionView(this);
        v.findViewById(R.id.quickButtonBar).setVisibility(View.GONE);

        mViewAnimator.removeAllViews();
        mViewAnimator.addView(v);
    }

    @Override
    public void onInteraction(InteractionManager.InteractionEvent event) {
        if (event != InteractionManager.InteractionEvent.NEGATIVE)
            switchView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LessonsFragment.OnLessonInteractionListener)
            mListener = (OnTrainingInteractionListener) context;
    }

    private void switchView() {
        View view = getNextInteractionView();

        if (view == null) {
            mListener.onTrainingInteraction(TrainingInteractionAction.FINISHED);
            return;
        }

        final ViewGroup parent = (ViewGroup)view.getParent();
        if (parent != null)
            parent.removeView(view);
        mViewAnimator.removeAllViews();
        mViewAnimator.addView(view);
    }

    protected abstract View getNextInteractionView();

    @Override
    public int getFloatingButtonVisibility() {
        return View.GONE;
    }

    enum TrainingInteractionAction {
        FINISHED
    }
    interface OnTrainingInteractionListener {
        void onTrainingInteraction(TrainingInteractionAction action);
    }
}

