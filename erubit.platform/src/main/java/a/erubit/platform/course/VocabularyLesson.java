package a.erubit.platform.course;

import android.support.annotation.NonNull;

public class VocabularyLesson extends BunchLesson {
    private static final int RANK_FAMILIAR = 1;
    private static final int RANK_LEARNED = 1;
    private static final int RANK_LEARNED_WELL = 1;

    VocabularyLesson(Course course) {
        super(course);
    }

    @Override
    protected int getRankFamiliar() {
        return RANK_FAMILIAR;
    }
    @Override
    protected int getRankLearned() {
        return RANK_LEARNED;
    }
    @Override
    protected int getRankLearnedWell() {
        return RANK_LEARNED_WELL;
    }


    @NonNull
    @Override
    protected PresentableDescriptor getPresentable(BunchLesson.Item problemItem) {
        return new PresentableDescriptor(new Problem(this, problemItem));
    }
}
