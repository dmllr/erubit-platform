package a.erubit.platform.course;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import u.U;

public class CharacterLesson extends BunchLesson {
    private static final int RANK_FAMILIAR = 1;
    private static final int RANK_LEARNED = 2;
    private static final int RANK_LEARNED_WELL = 3;

    CharacterLesson(Course course) {
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
    protected PresentableDescriptor getPresentable(Item problemItem) {
        Random random = new Random();
        int apxSize = 4 + random.nextInt(4);

        String regex = ",\\s+";
        Problem problem = new Problem(this, problemItem);
        String[] words = U.defurigana(problem.meaning).split(regex);
        problem.variants = new String[words.length + apxSize];

        ArrayList<String> variants = new ArrayList<>(problem.variants.length);
        variants.addAll(Arrays.asList(words));
        for (int k = 0; k < apxSize; k++) {
            String[] randomWords = U.defurigana(mSet.get(random.nextInt(mSet.size())).meaning).split(regex);
            for (String s : randomWords) {
                if (!variants.contains(s))
                    variants.add(s);
            }
        }

        // add noise if has empty slots
        for (int k = variants.size(); k < problem.variants.length; k++)
            variants.add(mVariants.get(random.nextInt(mVariants.size())));

        problem.variants = variants.toArray(problem.variants);

        U.shuffleStrArray(problem.variants);

        return new PresentableDescriptor(problem);
    }
}
