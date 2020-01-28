package a.erubit.platform.learning.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.lesson.BunchLesson
import u.C
import u.U
import java.util.*


class SetLesson constructor(course: Course) : CharacterLesson(course) {
	companion object {
		const val RANK_FAMILIAR = 2
		const val RANK_LEARNED = 3
		const val RANK_LEARNED_WELL = 5
	}

	override val rankFamiliar: Int
		get() = RANK_FAMILIAR
	override val rankLearned: Int
		get() = RANK_LEARNED
	override val rankLearnedWell: Int
		get() = RANK_LEARNED_WELL

	override fun getPresentable(problemItem: BunchLesson.Item): PresentableDescriptor {
		if (problemItem !is Item)
			return PresentableDescriptor.ERROR

		val problem = Problem(this, problemItem)

		val variants = mNoise ?: return PresentableDescriptor.ERROR
		val size = variants.size

		val answerIndices = IntArray(C.NUMBER_OF_ANSWERS)

		if (size > C.NUMBER_OF_ANSWERS) {
			var a = 0
			while (a < C.NUMBER_OF_ANSWERS) {
				val r = Random().nextInt(size)
				var ok = true
				for (k in 0 until C.NUMBER_OF_ANSWERS)
					ok = ok && answerIndices[k] != r
				if (ok) {
					answerIndices[a] = r
					a++
				}
			}
		} else {
			for (k in 0 until C.NUMBER_OF_ANSWERS)
				answerIndices[k] = k % C.NUMBER_OF_ANSWERS
			U.shuffleIntArray(answerIndices)
		}

		for (k in 0 until C.NUMBER_OF_ANSWERS)
			problem.variants[k] = variants[answerIndices[k]]

		pushCorrectAnswer(problemItem, problem)

		return PresentableDescriptor(problem)
	}

	private fun pushCorrectAnswer(item: Item, problem: Problem) {
		var ok = true
		for (k in 0 until C.NUMBER_OF_ANSWERS)
			ok = ok && U.defurigana(item.meaning) != problem.variants[k]

		if (ok) {
			val subst = Random().nextInt(C.NUMBER_OF_ANSWERS)
			problem.variants[subst] = U.defurigana(item.meaning)
		}
	}
}
