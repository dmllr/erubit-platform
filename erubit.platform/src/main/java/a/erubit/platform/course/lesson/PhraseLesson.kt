package a.erubit.platform.course.lesson

import a.erubit.platform.course.Course
import u.U
import java.util.*


class PhraseLesson internal constructor(course: Course) : CharacterLesson(course) {
	private val mRandom: Random = Random()
	private val mApxSize: Int = 3 + mRandom.nextInt(3)

	override val rankFamiliar: Int
		get() = 1
	override val rankLearned: Int
		get() = 2
	override val rankLearnedWell: Int
		get() = 3

	override fun getPresentable(problemItem: BunchLesson.Item): PresentableDescriptor {
		if (problemItem !is Item)
			return PresentableDescriptor.ERROR

		val problem = Problem(this, problemItem)
		val words = U.defurigana(problem.meaning).split("\\s+").toTypedArray()

		problem.variants = arrayOfNulls(words.size + mApxSize)
		System.arraycopy(words, 0, problem.variants, 0, words.size)

		val variants = mNoise ?: return PresentableDescriptor.ERROR

		for (k in 0 until mApxSize)
			problem.variants[words.size + k] = variants[mRandom.nextInt(variants.size)]

		U.shuffleStrArray(problem.variants)

		return PresentableDescriptor(problem)
	}
}
