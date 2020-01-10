package a.erubit.platform.course

class VocabularyLesson internal constructor(course: Course) : BunchLesson(course) {

	override val rankFamiliar: Int
		get() = 1
	override val rankLearned: Int
		get() = 1
	override val rankLearnedWell: Int
		get() = 1

	override fun getPresentable(problemItem: Item): PresentableDescriptor {
		return PresentableDescriptor(Problem(this, problemItem))
	}
}
