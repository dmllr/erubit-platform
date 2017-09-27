package a.erubit.platform.interaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import a.erubit.platform.R;
import a.erubit.platform.android.App;
import a.erubit.platform.course.CharacterLesson;
import a.erubit.platform.course.CourseManager;
import a.erubit.platform.course.Lesson;
import a.erubit.platform.course.PhraseLesson;
import a.erubit.platform.course.ProgressManager;
import a.erubit.platform.course.SetLesson;
import a.erubit.platform.course.VocabularyLesson;
import a.erubit.platform.course.WelcomeLesson;
import link.fls.swipestack.SwipeHelper;
import t.FlipLayout.FlipLayout;
import t.SwipeStack;
import t.TinyDB;
import u.C;
import u.U;

import static a.erubit.platform.android.App.getContext;

public class InteractionManager {
    @SuppressLint("StaticFieldLeak")
    private static final InteractionManager ourInstance = new InteractionManager();

    private final WindowManager.LayoutParams defaultLayoutParams;

    public static InteractionManager i() {
        return ourInstance;
    }

    private View mInteractionViewWelcome;
    private View mInteractionViewSetCourse;
    private View mInteractionViewPhraseCourse;
    private int[] mAnswerLabels;

    private InteractionViewData mLastViewData;

    public enum InteractionEvent {
        POSITIVE,
        NEGATIVE,
        CLOSE,
        BUSY1,
        BUSY2,
        BUSY4
    }
    public interface InteractionListener {
        void onInteraction(InteractionEvent event);
    }

    public void initialize() {
        createInteractionViews();
    }

    @SuppressWarnings("deprecation")
    private InteractionManager() {
        defaultLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    :
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | (
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT ?
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                        :
                        0
                ),
                PixelFormat.TRANSLUCENT);

        createInteractionViews();
    }

    private void createInteractionViews() {
        mInteractionViewWelcome = createWelcomeInteractionView();
        mInteractionViewSetCourse = createChooseInteractionView();
        mInteractionViewPhraseCourse = createPhraseInteractionView();
    }

    private View createWelcomeInteractionView() {
        return View.inflate(App.getContext(), R.layout.view_welcome, null);
    }

    private View createChooseInteractionView() {
        View view = View.inflate(App.getContext(), R.layout.view_interaction_choose, null);

        String packageName = App.getContext().getPackageName();
        mAnswerLabels = new int[C.NUMBER_OF_ANSWERS];
        for (int k = 0; k < C.NUMBER_OF_ANSWERS; k++)
            mAnswerLabels[k] = App.getContext().getResources().getIdentifier("a" + String.valueOf(k), "id", packageName);

        return view;
    }

    private View createPhraseInteractionView() {
        return View.inflate(App.getContext(), R.layout.view_interaction_phrase, null);
    }

    public View getInteractionView(InteractionListener listener) {
        Lesson lesson = CourseManager.i().getNextLesson();
        if (lesson != null)
            return getInteractionView(lesson, listener);
        return null;
    }

    public View getInteractionView(Lesson lesson, InteractionListener listener) {
        Lesson.PresentableDescriptor pd = lesson.getNextPresentable();

        if (pd.mStatus == Lesson.Status.LESSON_LEARNED) {
            // We got lesson with interaction enabled, but not presentable,
            // lesson may be learned or familiar at that moment.
            // Recalculate and save it's progress
            ProgressManager.i().save(lesson);

            // Will take next lesson and presentable
            lesson = CourseManager.i().getNextLesson();
            if (lesson != null)
                pd = lesson.getNextPresentable();
            else
                pd = Lesson.PresentableDescriptor.ERROR;
        }

        if (pd.mStatus == Lesson.Status.OK) {
            View view = InteractionManager.i().populate(lesson, pd, listener);

            assert view != null;
            final ViewGroup parent = (ViewGroup)view.getParent();
            if (parent != null)
                parent.removeView(view);

            return view;
        }

        return null;
    }

    public View getLastInteractionView(InteractionListener listener) {
        return InteractionManager.i().populate(mLastViewData.mLesson, mLastViewData.mPresentableDescription, listener);
    }

    private void onInteraction(InteractionListener listener, InteractionEvent event) {
        if (listener != null)
            listener.onInteraction(event);
    }

    public View populate(Lesson lesson, Lesson.PresentableDescriptor pd, InteractionListener listener) {
        View view = null;

        if (lesson == null || pd.mStatus != Lesson.Status.OK)
            return null;
        // TODO toast

        String lessonType = lesson.getClass().toString();
        if (lessonType.equals(WelcomeLesson.class.toString()))
            view = populateWelcome(lesson, (String) pd.mPresentable, listener);
        if (lessonType.equals(SetLesson.class.toString())) //noinspection ConstantConditions
            view = populateSetLesson(lesson, (Lesson.Problem) pd.mPresentable, listener);
        if (lessonType.equals(PhraseLesson.class.toString()) || lessonType.equals(CharacterLesson.class.toString())) //noinspection ConstantConditions
            view = populatePhraseLesson(lesson, (Lesson.Problem) pd.mPresentable, listener);
        if (lessonType.equals(VocabularyLesson.class.toString())) //noinspection ConstantConditions
            view = populateVocabularyLesson(lesson, (Lesson.Problem) pd.mPresentable, listener);

        assert view != null;
        view.setLayoutParams(defaultLayoutParams);

        mLastViewData = new InteractionViewData(lesson, pd);

        return view;
    }

    private View populateWelcome(final Lesson lesson, String text, final InteractionListener listener) {
        final View view = mInteractionViewWelcome;

        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText(U.getSpanned(text));

        view.findViewById(android.R.id.button1).setOnClickListener(v -> {
            lesson.mProgress.trainDate = lesson.mProgress.interactionDate = System.currentTimeMillis();
            ProgressManager.i().save(lesson);

            onInteraction(listener, InteractionEvent.POSITIVE);
        });


        setupQuickButtons(view, lesson, listener);

        return view;
    }

    private View populateSetLesson(final Lesson lesson, final Lesson.Problem problem, final InteractionListener listener) {
        final View view = mInteractionViewSetCourse;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        populateCard(view, problem);

        View.OnClickListener answerTouchListener = v -> {
            String answer = ((TextView)v).getText().toString();
            boolean solved = U.equals(problem.meaning, answer);

            lesson.mProgress.interactionDate = System.currentTimeMillis();
            problem.attempt(solved);

            if (solved) {
                lesson.mProgress.trainDate = System.currentTimeMillis();
                problem.treatResult();
                ProgressManager.i().save(lesson);

                onInteraction(listener, InteractionEvent.POSITIVE);
            }
            else {
                vibrateAsFail();
                U.animateNegative(view.findViewById(R.id.card));
                onInteraction(listener, InteractionEvent.NEGATIVE);
            }
        };

        final SwipeStack swipeStack = view.findViewById(R.id.swipeStack);

        View viewVariants = inflater.inflate(R.layout.view_card_variants, swipeStack, false);
        for (int i = 0; i < mAnswerLabels.length; i++) {
            Button tv = viewVariants.findViewById(mAnswerLabels[i]);
            tv.setText(((SetLesson.Problem)problem).variants[i]);
            tv.setOnClickListener(answerTouchListener);
        }
        viewVariants.findViewById(R.id.mistaken).setOnClickListener(v -> {
            lesson.mProgress.interactionDate = System.currentTimeMillis();
            problem.attempt(false);
            swipeStack.swipeTopViewToLeft();
        });

        View viewMeaning = inflater.inflate(R.layout.view_card_explaination, swipeStack, false);
        ((TextView) viewMeaning.findViewById(R.id.meaning)).setText(problem.meaning);

        final SetLesson.Knowledge problemKnowledge = ((SetLesson.Problem) problem).getKnowledge();
        if (problemKnowledge == SetLesson.Knowledge.Untouched)
            swipeStack.setAdapter(new SetLessonWelcomeStackAdapter(viewMeaning));
        else
            swipeStack.setAdapter(new SetLessonFullStackAdapter(viewVariants, viewMeaning));
        swipeStack.setListener(new link.fls.swipestack.SwipeStack.SwipeStackListener() {
            @Override
            public void onViewSwipedToLeft(int position) {
                if (problemKnowledge == SetLesson.Knowledge.Untouched) {
                    lesson.mProgress.interactionDate = lesson.mProgress.trainDate = System.currentTimeMillis();
                    problem.attempt(true);
                    problem.treatResult();
                    ProgressManager.i().save(lesson);
                    return;
                }
                switch (position) {
                    case 0:
                        lesson.mProgress.interactionDate = System.currentTimeMillis();
                        problem.attempt(false);
                        problem.treatResult();
                        ProgressManager.i().save(lesson);

                        View v = swipeStack.getAdapter().getView(1, null, null);
                        SwipeHelper sh = new SwipeHelper(swipeStack);
                        sh.registerObservedView(v, 0, 0);
                        sh.swipeViewToLeft();

                        break;
                }
            }

            @Override
            public void onViewSwipedToRight(int position) { }

            @Override
            public void onStackEmpty() {
                problem.treatResult();
                ProgressManager.i().save(lesson);

                onInteraction(listener, InteractionEvent.POSITIVE);
            }
        });
        swipeStack.resetStack();

        setupQuickButtons(view, lesson, listener);

        return view;
    }

    private View populateVocabularyLesson(final Lesson lesson, final Lesson.Problem problem, final InteractionListener listener) {
        final View view = mInteractionViewSetCourse;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        populateCard(view, problem);

        final SwipeStack swipeStack = view.findViewById(R.id.swipeStack);

        View viewMeaning = inflater.inflate(R.layout.view_card_explaination, swipeStack, false);
        TextView textMeaning = viewMeaning.findViewById(R.id.meaning);
        textMeaning.setText(problem.meaning);

        swipeStack.setAdapter(new SetLessonWelcomeStackAdapter(viewMeaning));
        swipeStack.setListener(new link.fls.swipestack.SwipeStack.SwipeStackListener() {
            public void onViewSwiped() {
                lesson.mProgress.interactionDate = lesson.mProgress.trainDate = System.currentTimeMillis();
                problem.attempt(true);
                problem.treatResult();
                ProgressManager.i().save(lesson);
            }

            @Override
            public void onViewSwipedToLeft(int position) {
                onViewSwiped();
            }

            @Override
            public void onViewSwipedToRight(int position) {
                onViewSwiped();
            }

            @Override
            public void onStackEmpty() {
                problem.treatResult();
                ProgressManager.i().save(lesson);

                onInteraction(listener, InteractionEvent.POSITIVE);
            }
        });
        swipeStack.resetStack();

        setupQuickButtons(view, lesson, listener);

        return view;
    }

    private View populatePhraseLesson(final Lesson lesson, final Lesson.Problem problem, final InteractionListener listener) {
        final View view = mInteractionViewPhraseCourse;

        populateCard(view, problem);

        final LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final t.FlowLayout fllPhrase = view.findViewById(R.id.phrase);
        final t.FlowLayout fllVariants = view.findViewById(R.id.variants);
        while (fllPhrase.getChildCount() > 1)
            fllPhrase.removeViewAt(0);
        while (fllVariants.getChildCount() > 1)
            fllVariants.removeViewAt(0);
        for (String variant: ((PhraseLesson.Problem)problem).variants) {
            View tpi = inflater.inflate(R.layout.text_phrase_item, fllVariants, false);
            TextView tv = tpi.findViewById(android.R.id.text1);
            tv.setText(variant);
            tpi.setOnClickListener(v -> animateMoveWord(view, v));

            fllVariants.addView(tpi, fllVariants.getChildCount() - 1);
        }

        view.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String answer = "";
            String separator = " ";
            if (lesson instanceof CharacterLesson)
                separator = ", ";

            for (int k = 0; k < fllPhrase.getChildCount() - 1; k++) {
                answer += (answer.length() > 0 ? separator : "")
                        + ((TextView) fllPhrase.getChildAt(k).findViewById(android.R.id.text1)).getText();
            }

            boolean solved = false;
            if (lesson instanceof PhraseLesson)
                solved = U.equals(problem.meaning, answer);
            if (lesson instanceof CharacterLesson)
                solved = U.equalsIndependent(problem.meaning, answer);


            problem.attempt(solved);

            if (solved) {
                problem.treatResult();
                ProgressManager.i().save(lesson);

                onInteraction(listener, InteractionEvent.POSITIVE);
            }
            else {
                vibrateAsFail();
                U.animateNegative(view.findViewById(R.id.card));
            }
        });

        setupQuickButtons(view, lesson, listener);

        return view;
    }

    private void populateCard(final View view, final Lesson.Problem problem) {
        FlipLayout card = view.findViewById(R.id.card);
        card.reset();
        card.setOnFlipListener(new FlipLayout.OnFlipListener() {
            @Override
            public void onFlipStart(FlipLayout view) {

            }

            @Override
            public void onFlipEnd(FlipLayout view) {
                if (view.getIsFlipped())
                    problem.spied();
            }
        });


        ((TextView) view.findViewById(R.id.problem)).setText(problem.text);
        ((TextView) view.findViewById(R.id.meaning)).setText(problem.meaning);
    }

    private void animateMoveWord(final View container, final View view) {
        Rect r0 = new Rect(), r1 = new Rect();

        view.getGlobalVisibleRect(r0);

        final ViewGroup parent = (ViewGroup)view.getParent();
        final ViewGroup dest = container.findViewById(parent.getId() == R.id.variants ? R.id.phrase : R.id.variants);

        if (dest.getChildCount() > 0) {
            View lastChild = dest.getChildAt(dest.getChildCount() - 1);
            lastChild.getGlobalVisibleRect(r1);
        } else
            dest.getGlobalVisibleRect(r1);

        TranslateAnimation anim = new TranslateAnimation(
                0, r1.left - r0.left,
                0, r1.top - r0.top);
        anim.setDuration(100);

        anim.setAnimationListener(new TranslateAnimation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                view.post(() -> {
                    parent.removeView(view);
                    dest.addView(view, dest.getChildCount() - 1);
                });
            }
        });

        view.startAnimation(anim);
    }

    private void setupQuickButtons(View view, final Lesson lesson, final InteractionListener listener) {
        view.findViewById(android.R.id.closeButton).setOnClickListener(v -> onInteraction(listener, InteractionEvent.CLOSE));
        View.OnClickListener disableForListener = v -> {
            InteractionEvent event = InteractionEvent.CLOSE;
            int id = v.getId();
            if (id == R.id.disableFor1h)
                event = InteractionEvent.BUSY1;
            else if (id == R.id.disableFor2h)
                event = InteractionEvent.BUSY2;
            else if (id == R.id.disableFor4h)
                event = InteractionEvent.BUSY4;
            onInteraction(listener, event);

            if (lesson.mProgress.interactionDate == 0) {
                lesson.mProgress.interactionDate = System.currentTimeMillis();
                ProgressManager.i().save(lesson);
            }
        };
        view.findViewById(R.id.disableFor1h).setOnClickListener(disableForListener);
        view.findViewById(R.id.disableFor2h).setOnClickListener(disableForListener);
        view.findViewById(R.id.disableFor4h).setOnClickListener(disableForListener);
    }

    private void vibrateAsFail() {
        long[] pattern = {0, 200, 100, 200};
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        createInteractionViews();
    }


    private static class SetLessonFullStackAdapter extends BaseAdapter {

        private final boolean shadowingEnabled;
        private final View mViewDoYouKnow;
        private final View mViewVariants;
        private final View mViewExplanation;

        SetLessonFullStackAdapter(View viewVariants, View viewExplanation) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            this.shadowingEnabled = new TinyDB(App.getContext()).getBoolean(C.SP_ENABLED_SHADOWING, true);

            this.mViewDoYouKnow = inflater.inflate(R.layout.view_card_know_dont_know,
                    (ViewGroup) viewVariants.getParent(), false);
            this.mViewVariants = viewVariants;
            this.mViewExplanation = viewExplanation;
        }

        @Override
        public int getCount() {
            return shadowingEnabled? 3 : 2;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            int vpos = position + (shadowingEnabled? 0 : 1);

            switch (vpos) {
                case 0:
                    return mViewDoYouKnow;
                case 1:
                    return mViewVariants;
                case 2:
                    return mViewExplanation;
            }
            return null;
        }
    }

    private static class SetLessonWelcomeStackAdapter extends BaseAdapter {

        private final View mViewExplanation;

        SetLessonWelcomeStackAdapter(View viewExplanation) {
            this.mViewExplanation = viewExplanation;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            switch (position) {
                case 0:
                    return mViewExplanation;
            }
            return null;
        }
    }

    private class InteractionViewData {
        private final Lesson mLesson;
        private final Lesson.PresentableDescriptor mPresentableDescription;

        InteractionViewData(Lesson lesson, Lesson.PresentableDescriptor pd) {
            this.mLesson = lesson;
            this.mPresentableDescription = pd;
        }
    }
}
