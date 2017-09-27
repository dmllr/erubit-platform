package a.erubit.platform.course;

import android.support.annotation.NonNull;

import java.util.Random;

import u.C;
import u.U;

public class SetLesson extends BunchLesson {
    public static final int RANK_FAMILIAR = 2;
    public static final int RANK_LEARNED = 3;
    public static final int RANK_LEARNED_WELL = 5;

    SetLesson(Course course) {
        super(course);
    }

    protected int getRankFamiliar() {
        return RANK_FAMILIAR;
    }
    protected int getRankLearned() {
        return RANK_LEARNED;
    }
    protected int getRankLearnedWell() {
        return RANK_LEARNED_WELL;
    }

    @NonNull
    @Override
    protected PresentableDescriptor getPresentable(BunchLesson.Item problemItem) {
        BunchLesson.Problem problem = new BunchLesson.Problem(this, problemItem);

        int size = mVariants.size();

        int[] answerIndices = new int[C.NUMBER_OF_ANSWERS];
        if (size > C.NUMBER_OF_ANSWERS) {
            int a = 0;
            while (a < C.NUMBER_OF_ANSWERS) {
                int r = new Random().nextInt(size);
                boolean ok = true;
                for (int k = 0; k < C.NUMBER_OF_ANSWERS; k++)
                    ok = ok && answerIndices[k] != r;
                if (ok) {
                    answerIndices[a] = r;
                    a++;
                }
            }
        } else {
            for (int k = 0; k < C.NUMBER_OF_ANSWERS; k++)
                answerIndices[k] = k % C.NUMBER_OF_ANSWERS;
            U.shuffleIntArray(answerIndices);
        }
        for (int k = 0; k < C.NUMBER_OF_ANSWERS; k++)
            problem.variants[k] = mVariants.get(answerIndices[k]);

        pushCorrectAnswer(problemItem, problem);

        return new PresentableDescriptor(problem);
    }

    private void pushCorrectAnswer(Item item, Problem problem) {
        boolean ok = true;
        for (int k = 0; k < C.NUMBER_OF_ANSWERS; k++)
            ok = ok && !U.defurigana(item.meaning).equals(problem.variants[k]);
        if (ok) {
            int subst = new Random().nextInt(C.NUMBER_OF_ANSWERS);
            problem.variants[subst] = U.defurigana(item.meaning);
        }
    }

}
