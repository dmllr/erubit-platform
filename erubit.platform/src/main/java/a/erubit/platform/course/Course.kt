package a.erubit.platform.course

import a.erubit.platform.course.lesson.Lesson
import java.util.*


abstract class Course {
	var id: String? = null
	var name: String? = null
	var description: String? = null
	var defaultActive = false
	var lessons: Map<String, Lesson> = LinkedHashMap(0)

	abstract val progress: Progress?

	fun getLesson(id: String): Lesson? {
		return lessons[id]
	}
}
