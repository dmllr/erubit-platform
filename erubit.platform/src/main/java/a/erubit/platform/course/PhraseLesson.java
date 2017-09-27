package a.erubit.platform.course;

import android.support.annotation.NonNull;

import java.util.Random;

import u.U;

public class PhraseLesson extends BunchLesson {
    private static final int RANK_FAMILIAR = 1;
    private static final int RANK_LEARNED = 2;
    private static final int RANK_LEARNED_WELL = 3;

    private final Random mRandom;
    private final int mApxSize;

    PhraseLesson(Course course) {
        super(course);

        mRandom = new Random();
        mApxSize = 3 + mRandom.nextInt(3);
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
    protected PresentableDescriptor getPresentable(Item problemItem) {
        Problem problem = new Problem(this, problemItem);
        String[] words = U.defurigana(problem.meaning).split("\\s+");
        problem.variants = new String[words.length + mApxSize];
        System.arraycopy(words, 0, problem.variants, 0, words.length);
        for (int k = 0; k < mApxSize; k++)
            problem.variants[words.length + k] = mVariants.get(mRandom.nextInt(mVariants.size()));

        U.shuffleStrArray(problem.variants);

        return new PresentableDescriptor(problem);
    }
}
