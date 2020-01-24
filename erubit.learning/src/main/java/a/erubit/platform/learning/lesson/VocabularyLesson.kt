package a.erubit.platform.learning.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.lesson.BunchLesson


class VocabularyLesson constructor(course: Course) : CharacterLesson(course) {

	override val rankFamiliar: Int
		get() = 1
	override val rankLearned: Int
		get() = 1
	override val rankLearnedWell: Int
		get() = 1

	override fun getPresentable(problemItem: BunchLesson.Item): PresentableDescriptor {
		if (problemItem !is Item)
			return PresentableDescriptor.ERROR

		return PresentableDescriptor(Problem(this, problemItem))
	}
}
