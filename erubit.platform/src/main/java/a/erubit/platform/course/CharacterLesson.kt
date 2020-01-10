package a.erubit.platform.course

import u.U
import java.util.*


class CharacterLesson internal constructor(course: Course) : BunchLesson(course) {
	override val rankFamiliar: Int
		get() = 1
	override val rankLearned: Int
		get() = 2
	override val rankLearnedWell: Int
		get() = 3

	override fun getPresentable(problemItem: Item): PresentableDescriptor {
		val random = Random()
		val apxSize = 4 + random.nextInt(4)
		val regex = "\\s+:\\s+"
		val problem = Problem(this, problemItem)
		val words = U.defurigana(problem.meaning).split(regex).toTypedArray()

		problem.variants = arrayOfNulls(words.size + apxSize)

		val variants = ArrayList<String>(problem.variants.size)
		variants.addAll(listOf(*words))

		val set = mSet ?: return PresentableDescriptor.ERROR
		val noise = mVariants ?: return PresentableDescriptor.ERROR

		for (k in 0 until apxSize) {
			val randomWords = U.defurigana(set[random.nextInt(set.size)].meaning).split(regex).toTypedArray()
			for (s in randomWords)
				if (!variants.contains(s))
					variants.add(s)
		}

		// add noise if has empty slots
		for (k in variants.size until problem.variants.size)
			variants.add(noise[random.nextInt(noise.size)])

		problem.variants = variants.toArray(problem.variants)
		U.shuffleStrArray(problem.variants)

		return PresentableDescriptor(problem)
	}
}
