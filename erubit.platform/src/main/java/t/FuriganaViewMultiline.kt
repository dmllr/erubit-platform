package t

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import java.util.*
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt


class FuriganaViewMultiline : TextView {
	private val mLines: Vector<Line> = Vector(0)
	private val mAllTexts: Vector<PairText> = Vector(0)

	private lateinit var mNormalTextPaint: TextPaint
	private lateinit var mFuriganaTextPaint: TextPaint
	private lateinit var mBoldTextPaint: TextPaint
	private lateinit var mItalicTextPaint: TextPaint
	private lateinit var mBoldItalicTextPaint: TextPaint

	private var mLineHeight = 0f
	private var mMaxLineWidth = 0f


	constructor(context: Context?) : super(context) {
		initialize()
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
		initialize()
	}

	constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
		initialize()
	}

	override fun setText(text: CharSequence, type: BufferType) {
		super.setText(text, type)

		if (text.isNotEmpty()) {
			setJText()
			onMeasure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
		}
	}

	private fun initialize() {
		val textPaint = paint

		mNormalTextPaint = TextPaint(textPaint)

		mFuriganaTextPaint = TextPaint(textPaint)
		mFuriganaTextPaint.textSize = textPaint.textSize / 2

		mBoldTextPaint = TextPaint(textPaint)
		mBoldTextPaint.isFakeBoldText = true

		mItalicTextPaint = TextPaint(textPaint)
		mItalicTextPaint.textSkewX = -0.35f

		mBoldItalicTextPaint = TextPaint(textPaint)
		mBoldItalicTextPaint.textSkewX = -0.35f
		mBoldItalicTextPaint.isFakeBoldText = true

		// Calculate the height of one line.
		mLineHeight = mFuriganaTextPaint.fontSpacing + 8 +
			maxOf(
				mNormalTextPaint.fontSpacing,
				max(mBoldTextPaint.fontSpacing, mItalicTextPaint.fontSpacing),
				mBoldItalicTextPaint.fontSpacing
			)

		setJText()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		val widthSize = MeasureSpec.getSize(widthMeasureSpec)
		val heightSize = MeasureSpec.getSize(heightMeasureSpec)

		measureText(widthSize)

		var height = ceil(mLineHeight * mLines.size).roundToInt() + 15
		var width = widthSize

		if (widthMode != MeasureSpec.EXACTLY && mLines.size <= 1)
			width = ceil(mMaxLineWidth.toDouble()).roundToInt()
		if (heightMode != MeasureSpec.UNSPECIFIED && height > heightSize)
			height = height or View.MEASURED_STATE_TOO_SMALL

		setMeasuredDimension(width, height)
	}

	/***
	 * Measure view with max width.
	 * @param width if width < 0 → the view has one line, which has width unlimited.
	 */
	private fun measureText(width: Int) {
		mLines.clear()
		mMaxLineWidth = 0f

		if (width <= 0) {
			val line = Line()
			line.mPairTexts = mAllTexts
			for (t in mAllTexts)
				mMaxLineWidth += t.mWidth
		} else {
			var tmpW = 0f
			var pairTexts = Vector<PairText>()

			for (pairText in mAllTexts) {
				// Break to new line if {@PairText} contain break character.
				if (pairText.isBreak) {
					val line = Line()
					line.mPairTexts = pairTexts
					mLines.add(line)
					mMaxLineWidth = if (mMaxLineWidth > tmpW) mMaxLineWidth else tmpW

					// Reset for new line
					pairTexts = Vector()
					tmpW = 0f

					continue
				}

				tmpW += pairText.mWidth

				if (tmpW < width) {
					pairTexts.add(pairText)
				} else {
					var line = Line()
					tmpW -= pairText.mWidth

					// If kanji -> break to new line
					if (pairText.mFuriganaText != null) {
						line.mPairTexts = pairTexts
						mLines.add(line)
						mMaxLineWidth = if (mMaxLineWidth > tmpW) mMaxLineWidth else tmpW

						// Reset for new line
						pairTexts = Vector()
						pairTexts.add(pairText)
						tmpW = pairText.mWidth
					} else {
						var splitPairText = pairText.split(tmpW, width.toFloat(), pairTexts)

						// Add new line
						line.mPairTexts = pairTexts
						mLines.add(line)
						mMaxLineWidth = if (mMaxLineWidth > tmpW) mMaxLineWidth else tmpW
						if (splitPairText == null) {
							pairTexts = Vector()
							continue
						}

						// Reset for new line
						tmpW = splitPairText.mWidth

						//split for long text
						while (tmpW > width) {
							tmpW = 0f
							pairTexts = Vector()
							splitPairText = splitPairText!!.split(tmpW, width.toFloat(), pairTexts)

							line = Line()
							line.mPairTexts = pairTexts
							mLines.add(line)

							mMaxLineWidth = if (mMaxLineWidth > tmpW) mMaxLineWidth else tmpW
							tmpW = splitPairText?.mWidth ?: 0f
						}

						pairTexts = Vector()

						if (splitPairText != null) {
							pairTexts.add(splitPairText)
						}
					}
				}

				// Make the rest of text before quit loop.
				if (pairText == mAllTexts.lastElement() && pairTexts.size > 0) {
					val line = Line()
					line.mPairTexts = pairTexts
					mLines.add(line)

					mMaxLineWidth = if (mMaxLineWidth > tmpW) mMaxLineWidth else tmpW
				}
			}
		}
	}

	override fun onDraw(canvas: Canvas) {
		if (mLines.size > 0) {
			var y = mLineHeight
			for (line in mLines) {
				line.mLineRect.set(0, (y - mLineHeight).toInt(), mMaxLineWidth.toInt(), y.toInt())

				var x = 0f
				for (pairText in line.mPairTexts) {
					pairText.mPairRect.set(x.toInt(), (y - mLineHeight).toInt(), (x + pairText.mWidth).toInt(), y.toInt())
					pairText.onDraw(canvas, x, y)
					x += pairText.mWidth
				}
				y += mLineHeight
			}
		} else
			super.onDraw(canvas)
	}

	/**
	 * Set text without invalidate
	 */
	private fun setJText() {
		val t = text.toString()
		if (t.isEmpty())
			return

		mAllTexts.clear()
		parseBoldText(t.replace(BREAK_REGEX.toRegex(), BREAK_CHARACTER))
	}

	/**
	 * Parse text with struct **...**
	 * @param text text to parse
	 */
	private fun parseBoldText(text: String) {
		val pattern = Pattern.compile(BOLD_TEXT_REGEX)
		val matcher = pattern.matcher(text)

		var start = 0
		var end: Int

		while (matcher.find()) {
			val fullText = matcher.group(1) ?: ""
			val boldText = matcher.group(3) ?: ""

			end = text.indexOf(fullText, start)

			if (end < 0)
				continue

			if (end > start)
				parseItalicText(text.substring(start, end), TYPE_NORMAL)

			parseItalicText(boldText, TYPE_BOLDER)

			start = end + fullText.length
		}
		end = text.length

		if (end > start)
			parseItalicText(text.substring(start, end), TYPE_NORMAL)
	}

	/**
	 * Parse text with struct *...*
	 * @param text text to parse
	 * @param type 2 type for this function. bold and normal.
	 */
	private fun parseItalicText(text: String, type: Int) {
		val pattern = Pattern.compile(ITALIC_TEXT_REGEX)
		val matcher = pattern.matcher(text)

		var start = 0
		var end: Int

		while (matcher.find()) {
			val fullText = matcher.group(1) ?: ""
			val italicText = matcher.group(3) ?: ""

			end = text.indexOf(fullText, start)

			if (end < 0)
				continue

			if (end > start)
				parseFuriganaText(text.substring(start, end), type)

			parseFuriganaText(italicText, if (type == TYPE_BOLDER) TYPE_BOLDER or TYPE_ITALIC else TYPE_ITALIC)

			start = end + fullText.length
		}
		end = text.length

		if (end > start)
			parseFuriganaText(text.substring(start, end), type)
	}

	/**
	 * Parse text with struct {kanji:furigana}
	 * @param text text to parse
	 * @param type 4 type for display. bold, italic, bold-italic and normal.
	 */
	private fun parseFuriganaText(text: String, type: Int) {
		val pattern = Pattern.compile(KANJI_REGEX)
		val matcher = pattern.matcher(text)

		var start = 0
		var end: Int

		while (matcher.find()) {
			val fullText = matcher.group(1) ?: ""
			val kanji = matcher.group(3) ?: ""
			val furigana = matcher.group(5) ?: ""

			end = text.indexOf(fullText, start)

			if (end < 0)
				continue

			if (end > start)
				parseBreakLineText(text.substring(start, end), type)

			val kanjiText = JText(kanji, type)
			val furiganaText = FuriganaText(furigana)
			val pairText = PairText(kanjiText, furiganaText)

			mAllTexts.add(pairText)

			start = end + fullText.length
		}
		end = text.length

		if (end > start)
			parseBreakLineText(text.substring(start, end), type)
	}

	/**
	 * Parse text with struct \n \r <br></br> <br></br> <br></br>
	 * @param text text to parse
	 * @param type 4 type for display. bold, italic, bold-italic and normal.
	 */
	private fun parseBreakLineText(text: String, type: Int) {
		if (text.contains(BREAK_CHARACTER)) {
			val breakIndex = text.indexOf(BREAK_CHARACTER)
			val jText = JText(text.substring(0, breakIndex), type)

			mAllTexts.add(PairText(jText))
			mAllTexts.add(PairText())

			val secondText = text.substring(breakIndex + BREAK_CHARACTER.length)
			parseBreakLineText(secondText, type)
		} else
			mAllTexts.add(PairText(JText(text, type)))
	}


	private inner class Line {
		var mPairTexts: Vector<PairText> = Vector(0)
		val mLineRect: Rect = Rect()
	}


	private inner class PairText {
		var mPairRect: Rect = Rect()
		lateinit var mJText: JText
		var mFuriganaText: FuriganaText? = null
		var isBreak = false

		var mWidth = 0f

		constructor() {
			isBreak = true
		}

		constructor(jText: JText) {
			mJText = jText
			measureWidth()
		}

		constructor(jText: JText, furiganaText: FuriganaText?) {
			mJText = jText
			mFuriganaText = furiganaText
			measureWidth()
		}

		private fun measureWidth() {
			mWidth = when (mFuriganaText) {
				null -> mJText.mWidth
				else -> max(mJText.mWidth, mFuriganaText!!.mWidth)
			}
		}

		fun split(width: Float, maxWidth: Float, pairTexts: Vector<PairText>): PairText? {
			return if (mFuriganaText != null) null else mJText.split(width, maxWidth, pairTexts)
		}

		fun onDraw(canvas: Canvas, x: Float, y: Float) {
			if (mFuriganaText == null)
				mJText.onDraw(canvas, x, y)
			else {
				val nx = x + (mWidth - mJText.mWidth) / 2
				mJText.onDraw(canvas, nx, y)

				val fx = x + (mWidth - mFuriganaText!!.mWidth) / 2
				val fy = y - mJText.mHeight
				mFuriganaText!!.onDraw(canvas, fx, fy)
			}
		}
	}


	private open inner class JText(var text: String, var type: Int) {
		var mWidth: Float
		var mHeight = 0f
		var mWidthCharArray: FloatArray = FloatArray(text.length)
		lateinit var mTextPaint: TextPaint

		fun onDraw(canvas: Canvas, x: Float, y: Float) {
			mTextPaint.color = currentTextColor
			canvas.drawText(text, 0, text.length, x, y, mTextPaint)
		}

		fun split(width: Float, maxWidth: Float, pairTexts: Vector<PairText>): PairText? {
			var w = width
			var i = 0
			while (i < mWidthCharArray.size) {
				w += mWidthCharArray[i]
				if (w < maxWidth) {
					i++
					continue
				} else {
					w -= mWidthCharArray[i]
					i--
					break
				}
			}
			return when {
				i <= 0 -> PairText(JText(text, type))
				else -> {
					val newText = text.substring(0, i)
					val pairText = PairText(JText(newText, type))
					pairTexts.add(pairText)

					when (i) {
						text.length -> null
						else -> PairText(JText(text.substring(i), type))
					}
				}
			}
		}

		init {
			when {
				type and TYPE_FURIGANA == TYPE_FURIGANA -> {
					mFuriganaTextPaint.getTextWidths(text, mWidthCharArray)
					mHeight = mFuriganaTextPaint.descent() - mFuriganaTextPaint.ascent()
					mTextPaint = mFuriganaTextPaint
				}
				type and TYPE_BOLDER == TYPE_BOLDER && type and TYPE_ITALIC == TYPE_ITALIC -> {
					mBoldItalicTextPaint.getTextWidths(text, mWidthCharArray)
					mHeight = mBoldItalicTextPaint.descent() - mBoldItalicTextPaint.ascent()
					mTextPaint = mBoldItalicTextPaint
				}
				type and TYPE_BOLDER == TYPE_BOLDER -> {
					mBoldTextPaint.getTextWidths(text, mWidthCharArray)
					mHeight = mBoldTextPaint.descent() - mBoldTextPaint.ascent()
					mTextPaint = mBoldTextPaint
				}
				type and TYPE_ITALIC == TYPE_ITALIC -> {
					mItalicTextPaint.getTextWidths(text, mWidthCharArray)
					mHeight = mItalicTextPaint.descent() - mItalicTextPaint.ascent()
					mTextPaint = mItalicTextPaint
				}
				type and TYPE_NORMAL == TYPE_NORMAL -> {
					mNormalTextPaint.getTextWidths(text, mWidthCharArray)
					mHeight = mNormalTextPaint.descent() - mNormalTextPaint.ascent()
					mTextPaint = mNormalTextPaint
				}
			}

			mWidth = 0f
			for (w in mWidthCharArray)
				mWidth += w
		}
	}


	private inner class FuriganaText(text: String) : JText(text, TYPE_FURIGANA)


	companion object {
		private const val TYPE_NORMAL = 1
		private const val TYPE_BOLDER = 2
		private const val TYPE_ITALIC = 4
		private const val TYPE_FURIGANA = 8

		/* regex for kanji with struct {kanji:furigana} */
		private const val KANJI_REGEX = "(([{])([ぁ-ゔゞァ-・ヽヾ゛゜ー一-龯０-９0-9]*)([:])([\\u3040-\\u30FFー[0-9]]*)([}]))"
		private const val BOLD_TEXT_REGEX = "(?m)(?d)(?s)(([<][b][>])(.*?)([<][\\/][b][>]))"
		private const val ITALIC_TEXT_REGEX = "(?m)(?d)(?s)(([<][i][>])(.*?)([<][\\/][i][>]))"
		private const val BREAK_REGEX = "(<br ?\\/?>)"
		private const val BREAK_CHARACTER = "\n"
	}
}
