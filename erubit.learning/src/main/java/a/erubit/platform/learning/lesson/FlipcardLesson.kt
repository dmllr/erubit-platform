package a.erubit.platform.learning.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.lesson.BunchLesson
import a.erubit.platform.course.lesson.Lesson
import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import u.U
import java.io.IOException
import java.util.*


class FlipcardLesson constructor(course: Course) : BunchLesson(course) {
    override val rankFamiliar: Int
        get() = 1
    override val rankLearned: Int
        get() = 2
    override val rankLearnedWell: Int
        get() = 3

    override fun getPresentable(problemItem: BunchLesson.Item): PresentableDescriptor {
        if (problemItem !is Item)
            return PresentableDescriptor.ERROR

        return PresentableDescriptor(Problem(this, problemItem))
    }

    override fun fromJson(context: Context, jo: JSONObject): BunchLesson {
        try {
            id = jo.getString("id")
            name = U.getStringValue(context, jo, "title")

            val progress = loadProgress(context)

            val jcards = jo.getJSONArray("cards")
            val cards = ArrayList<BunchLesson.Item>(jcards.length())
            for (i in 0 until jcards.length()) {
                val jso = jcards.getJSONObject(i)
                cards.add(Item().fromJsonObject(jso).withProgress(progress))
            }
            mSet = cards

            mProgress = progress
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return this
    }


    inner class Flipcard {
        val face: Face = Face()
        val back: Back = Back()

        inner class Face {
            var content: String = ""
            var helper: String = ""
            var side: String = ""
            var additions: ArrayList<String> = ArrayList(0)
        }

        inner class Back {
            var content: String = ""
        }
    }


    inner class Item : BunchLesson.Item() {
        lateinit var flipcard: Flipcard

        override fun fromJsonObject(jso: JSONObject): BunchLesson.Item {
            val fc = Flipcard()
            var jo = jso.getJSONObject("face")
            fc.face.content = jo.getString("content")
            fc.face.helper = jo.getString("helper")
            fc.face.side = jo.getString("side")
            val jadd = jo.getJSONArray("add")
            for (i in 0 until jadd.length())
                fc.face.additions.add(jadd.getString(i))

            jo = jso.getJSONObject("back")
            fc.back.content = jo.getString("content")

            title = fc.face.content

            flipcard = fc

            return this
        }
	}


    inner class Problem constructor(lesson: Lesson, item: Item) : BunchLesson.Problem(lesson, item) {
        val flipcard = item.flipcard
    }

}
