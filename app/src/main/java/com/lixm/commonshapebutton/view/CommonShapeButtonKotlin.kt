package com.lixm.commonshapebutton.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import com.lixm.commonshapebutton.R

/**
 * Describe:
 *
 * Author: Lixm
 * Date: 2018/9/6
 */
class CommonShapeButtonKotlin @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private companion object {
        val TOP_LEFT = 1
        val TOP_RIGHT = 2
        val BOTTOM_RIGHT = 4
        val BOTTOM_LEFT = 8
    }

    /**
     * shape模式
     * 矩形（rectangle）、椭圆形（oval）、线性（line）、环形（ring）
     */
    private var mShapeMode = 0
    private var mFillColor = 0 //填充颜色
    private var mPressedColor = 0 //按压颜色
    private var mStrokeColor = 0 //描边颜色
    private var mStrokeWidth = 0 //描边宽度
    private var mCornerRadius = 0 //圆角半径
    private var mCornerPosition = 0 //圆角位置
    private var mActiveEnable = false //点击动效
    private var mStartColor = 0 //起始颜色
    private var mEndColor = 0 //结束颜色

    /**
     * 渐变方向
     * 0-GradientDrawable.Orientation.TOP_BOTTOM
     * 1-GradientDrawable.Orientation.LEFT_RIGHT
     */
    private var mOrientation = 0
    /**
     * drawable位置
     * -1-null、0-left、1-top、2-right、3-bottom
     */
    private var mDrawablePosition = -1

    /**
     *普通shape样式
     */
    private val normalGradientDrawable: GradientDrawable by lazy { GradientDrawable() }

    /**
     * 按压shape样式
     */
    private val pressedGradientDrawable: GradientDrawable by lazy { GradientDrawable() }

    /**
     * shape样式集合
     */
    private val stateListDrawable: StateListDrawable by lazy { StateListDrawable() }

    //button内容总宽度
    private var contentWidth = 0f
    //button内容总高度
    private var contentHeight = 0f

    /**
     * 文字是否自适应
     */
    private var mTextIsFit: Boolean = false
    private var mTextPaint: Paint? = null
    private var mTextSize: Float = 0.toFloat()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CommonShapeButton).apply {
            mShapeMode = getInt(R.styleable.CommonShapeButton_csb_shapeMode, 0)
            mFillColor = getColor(R.styleable.CommonShapeButton_csb_fillColor, 0xFFFFFFFF.toInt())
            mPressedColor = getColor(R.styleable.CommonShapeButton_csb_pressedColor, 0xFF666666.toInt())
            mStrokeColor = getColor(R.styleable.CommonShapeButton_csb_strokeColor, 0)
            mStrokeWidth = getDimensionPixelSize(R.styleable.CommonShapeButton_csb_strokeWidth, 0)
            mCornerRadius = getDimensionPixelSize(R.styleable.CommonShapeButton_csb_cornerRadius, 0)
            mCornerPosition = getInt(R.styleable.CommonShapeButton_csb_drawablePosition, -1)
            mActiveEnable = getBoolean(R.styleable.CommonShapeButton_csb_activeEnable, false)
            mDrawablePosition = getInt(R.styleable.CommonShapeButton_csb_drawablePosition, -1)
            mStartColor = getColor(R.styleable.CommonShapeButton_csb_startColor, 0xFFFFFFFF.toInt())
            mEndColor = getColor(R.styleable.CommonShapeButton_csb_endColor, 0xFFFFFFFF.toInt())
            mOrientation = getColor(R.styleable.CommonShapeButton_csb_orientation, 0)
            mTextIsFit = getBoolean(R.styleable.CommonShapeButton_csb_text_is_fit, false)
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //初始化normal状态
        with(normalGradientDrawable) {
            //渐变色
            if (mStartColor != 0xFFFFFFFF.toInt() && mEndColor != 0xFFFFFFFF.toInt()) {
                colors = intArrayOf(mStartColor, mEndColor)
                when (mOrientation) {
                    0 -> orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    1 -> orientation = GradientDrawable.Orientation.LEFT_RIGHT
                }
            } else {//填充色
                setColor(mFillColor)
            }

            when (mShapeMode) {
                0 -> shape = GradientDrawable.RECTANGLE
                1 -> shape = GradientDrawable.OVAL
                2 -> shape = GradientDrawable.LINE
                3 -> shape = GradientDrawable.RING
            }
            //统一设置圆角半径
            if (mCornerPosition == -1) {
                cornerRadius = mCornerRadius.toFloat()
            } else { //根据圆角位置设置圆角半径
                cornerRadii = getCornerRadiusByPosition()
            }
            //默认的透明边框不绘制，否则会导致没有阴影
            if (mStrokeColor != 0)
                setStroke(mStrokeWidth, mStrokeColor)
        }

        //是否开启点击动效
        background = if (mActiveEnable) {
            //5.0 以上水波纹效果
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                RippleDrawable(ColorStateList.valueOf(mPressedColor), normalGradientDrawable, null)
            } else {//5.0 以下变色效果
                //初始化pressed状态
                with(pressedGradientDrawable) {
                    setColor(mPressedColor)
                    when (mShapeMode) {
                        0 -> shape = GradientDrawable.RECTANGLE
                        1 -> shape = GradientDrawable.OVAL
                        2 -> shape = GradientDrawable.LINE
                        3 -> shape = GradientDrawable.RING
                    }
                    cornerRadius = mCornerRadius.toFloat()
                    setStroke(mStrokeWidth, mStrokeColor)
                }
                //注意此处的add殊勋，normal必须在最后一个，否则其他状态无效
                // 设置pressed 状态
                stateListDrawable.apply {
                    addState(intArrayOf(android.R.attr.state_pressed), pressedGradientDrawable)
                    //设置normal状态
                    addState(intArrayOf(), normalGradientDrawable)
                }
            }
        } else {
            normalGradientDrawable
        }
    }

    /**
     * 根据圆角位置获取圆角半径
     */
    private fun getCornerRadiusByPosition(): FloatArray {
        val result = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val cornerRadius = mCornerRadius.toFloat()
        if (containsFlag(mCornerPosition, TOP_LEFT)) {
            result[0] = cornerRadius
            result[1] = cornerRadius
        }
        if (containsFlag(mCornerPosition, TOP_RIGHT)) {
            result[2] = cornerRadius
            result[3] = cornerRadius
        }
        if (containsFlag(mCornerPosition, BOTTOM_RIGHT)) {
            result[4] = cornerRadius
            result[5] = cornerRadius
        }
        if (containsFlag(mCornerPosition, BOTTOM_LEFT)) {
            result[6] = cornerRadius
            result[7] = cornerRadius
        }
        return result
    }

    /**
     * 是否包含对应flag
     */
    private fun containsFlag(flagSet: Int, flag: Int): Boolean {
        return flagSet or flag == flagSet
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        //如果xml中配置了drawable则设置padding让文字移动到边缘与drawable靠在一起
        //button中配置的drawable默认贴着边缘
        if (mDrawablePosition > -1) {
            compoundDrawables?.let {
                val drawable: Drawable? = compoundDrawables[mDrawablePosition]
                drawable?.let {
                    //图片间距
                    val drawablePadding = compoundDrawablePadding
                    when (mDrawablePosition) {
                    //左右drawable
                        0, 2 -> {
                            //图片宽度
                            val drawableWidth = it.intrinsicWidth
                            //获取文字宽度
                            val textWidth = paint.measureText(text.toString())
                            //内容总宽度
                            contentWidth = textWidth + drawableWidth + drawablePadding
                            val rightPadding = (width - contentWidth).toInt()
                            //图片和文字全部靠在左侧
                            setPadding(0, 0, rightPadding, 0)
                        }
                    //上下drawable
                        1, 3 -> {
                            //图片高度
                            val drawableHeight = it.intrinsicHeight
                            //获取文字高度
                            val fm = paint.fontMetrics
                            //单行高度
                            val singeLineHeight = Math.ceil(fm.descent.toDouble() - fm.ascent.toDouble()).toFloat()
                            //总的行间距
                            val totalLineSpaceHeight = (lineCount - 1) * lineSpacingExtra
                            val textHeight = singeLineHeight * lineCount + totalLineSpaceHeight
                            //内容总高度
                            contentHeight = textHeight + drawableHeight + drawablePadding
                            //图片和文字全部靠在上侧
                            val bottomPadding = (height - contentHeight).toInt()
                            setPadding(0, 0, 0, bottomPadding)
                        }
                    }
                }
            }
        }
        //内容居中
        gravity = Gravity.CENTER
        //可点击
        isClickable = true
    }

    override fun onDraw(canvas: Canvas?) {
        //让图片和文字居中
        when {
            contentWidth > 0 && (mDrawablePosition == 0 || mDrawablePosition == 2) -> canvas!!.translate((width - contentWidth) / 2, 0f)
            contentHeight > 0 && (mDrawablePosition == 1 || mDrawablePosition == 3) -> canvas!!.translate(0f, (height - contentHeight) / 2)
        }
        super.onDraw(canvas)
        refitText(this.text.toString(), this.width)
    }

    private fun refitText(text: String?, textViewWidth: Int) {
        if (!mTextIsFit || text == null || textViewWidth <= 0) {
            return
        }
        mTextPaint = Paint()
        mTextPaint!!.set(this.paint)
        val availableTextViewWidth = width - paddingLeft - paddingRight
        val charsWidthArr = FloatArray(text.length)
        val boundsRect = Rect()
        mTextPaint!!.getTextBounds(text, 0, text.length, boundsRect)
        var textWidth = boundsRect.width()
        mTextSize = textSize
        while (textWidth > availableTextViewWidth) {
            mTextSize -= 1f
            mTextPaint!!.setTextSize(mTextSize)
            textWidth = mTextPaint!!.getTextWidths(text, charsWidthArr)
        }
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize)
    }
}