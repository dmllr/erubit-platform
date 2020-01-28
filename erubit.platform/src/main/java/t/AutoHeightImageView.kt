package t

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView


class AutoHeightImageView : ImageView {
	constructor(context: Context?) : super(context)
	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
	constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		try {
			val drawable = drawable
			if (drawable == null) {
				setMeasuredDimension(0, 0)
			} else {
				val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
//				var measuredHeight = MeasureSpec.getSize(heightMeasureSpec)

				val measuredHeight = measuredWidth * drawable.intrinsicHeight / drawable.intrinsicWidth

				setMeasuredDimension(measuredWidth, measuredHeight)
			}
		} catch (e: Exception) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		}
	}
}
