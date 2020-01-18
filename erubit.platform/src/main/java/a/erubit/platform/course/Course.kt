package a.erubit.platform.course

import a.erubit.platform.course.lesson.Lesson
import java.util.*


abstract class Course {
	var id: String? = null
	var name: String? = null
	var description: String? = null
	var defaultActive = false
	var lessons: ArrayList<Lesson>? = null

	abstract val progress: Progress?

	fun getLesson(id: String): Lesson? {
		lessons ?: return null

		for (lesson in lessons!!)
			if (id == lesson.id)
				return lesson

		return null
	}
}
