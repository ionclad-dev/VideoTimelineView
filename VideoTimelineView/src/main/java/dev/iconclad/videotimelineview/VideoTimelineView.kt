package dev.iconclad.videotimelineview


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.FileInputStream
import kotlin.math.ceil


class VideoTimelineView : View {
    private var _listener: VideoTimelineViewListener? = null

    fun setListener(listener: VideoTimelineViewListener) {
        _listener = listener
    }

    private var _videoPath: String? = null
    private var _progress: Float = 0f
    private var _progressStart = 0f
    private var _progressEnd = 1f
    private var _videoDuration: Long = 0
    private val _durationSecond: Long
        get() = ((_progressEnd - _progressStart) * _videoDuration).toLong() / 1000
    private val durationText: String
        get() = "$_durationSecond ms"

    private var _showTime = true
    private var _playEnabled = true
    private var _pressedPlay = false
    private var _pressDx = 0f
    private var _pressedLeft = false
    private var _pressedRight = false

    private var _minProgressDiff = 0.0f
    private var _maxProgressDiff = 1.0f


    private var _frames: MutableList<Bitmap> = mutableListOf()


    private lateinit var _paint: Paint
    private lateinit var _paintShadow: Paint
    private lateinit var _paintText: Paint
    private val _rect = RectF()
    private val _rectCard = Rect()


    private var _drawableLeft: Drawable? = null
    private var _drawableRight: Drawable? = null
    private val _lineSize: Float = 2f
    private var _density = 1f

    private val _paddingTB: Float = dp(8f).toFloat()
    private val _paddingLR: Int
        get() = (width - _timelineWidth) / 2

    private val _frameWH: Int
        get() = height - dp(_paddingTB * 2)

    private val _timelineWidth: Int
        get() = _frameWH * _maxFrames

    private val _maxFrames: Int
        get() = width / _frameWH

    private fun dp(value: Float): Int {
        return if (value == 0f) {
            0
        } else ceil((_density * value)).toInt()
    }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }


    private fun init(attrs: AttributeSet?) {
        _drawableLeft = context.resources.getDrawable(R.drawable.video_cropleft, null)
        _drawableRight = context.resources.getDrawable(R.drawable.video_cropright, null)
        _density = context.resources.displayMetrics.density


        _paint = Paint(Paint.ANTI_ALIAS_FLAG)
        _paintShadow = Paint(Paint.ANTI_ALIAS_FLAG)

        _paintText = Paint(Paint.ANTI_ALIAS_FLAG)

        _paintText.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            12f,
            context.resources.displayMetrics
        )
        _paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)


        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoTimelineView)

        _playEnabled = typedArray.getBoolean(R.styleable.VideoTimelineView_playLine, true)

        _showTime = typedArray.getBoolean(R.styleable.VideoTimelineView_showTime, true)
        _paint.color = typedArray.getColor(R.styleable.VideoTimelineView_color, Color.WHITE)
        val iconColor = typedArray.getColor(R.styleable.VideoTimelineView_iconColor, Color.BLACK)
        _paintText.color = iconColor
        _paintShadow.color = iconColor
        _drawableRight!!.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY)
        _drawableLeft!!.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY)


        _progressStart = typedArray.getFloat(R.styleable.VideoTimelineView_startProgress, 0f)
        _progressEnd = typedArray.getFloat(R.styleable.VideoTimelineView_endProgress, 1f)
        _progress = typedArray.getFloat(R.styleable.VideoTimelineView_progress, 0f)


        typedArray.recycle()


    }

    fun setVideoPath(file: File) {
        val mediaMetadataRetriever = MediaMetadataRetriever()

        try {
            val inputStream = FileInputStream(file.absolutePath)
            mediaMetadataRetriever.setDataSource(inputStream.fd)
            val duration =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            _videoDuration = duration!!.toLong()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        invalidate()
    }

    fun setVideoPath(path: String) {
        _videoPath = path
        setVideoPath(File(path))
    }

    // frameleri boyutlandır
    private fun prepareFrame(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(_frameWH, _frameWH, bitmap.config)
        val canvas = Canvas(result)
        val scaleX: Float = _frameWH.toFloat() / bitmap.width.toFloat()
        val scaleY: Float = _frameWH.toFloat() / bitmap.height.toFloat()
        val scale = if (scaleX > scaleY) scaleX else scaleY
        val w = (bitmap.width * scale).toInt()
        val h = (bitmap.height * scale).toInt()
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val destRect = Rect((_frameWH - w) / 2, (_frameWH - h) / 2, w, h)
        canvas.drawBitmap(bitmap, srcRect, destRect, null)
        bitmap.recycle()
        return result
    }

    // frameleri getirir
    private fun decodeFrames(maxFrames: Int): MutableList<Bitmap> {
        if (_videoPath == null) return mutableListOf()
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(_videoPath)

        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
            .toLong() * 1000

        // Her bir frame'in zaman dilimini hesapla
        val frameInterval = duration / maxFrames

        val frames = mutableListOf<Bitmap>()

        for (i in 0 until maxFrames) {
            val timeUs = i * frameInterval
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frames.add(prepareFrame(frame!!))
        }

        retriever.release()

        return frames
    }

    fun setProgress(progress: Float) {
        this._progress = progress
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        // timeline frames
        if (_frames.isEmpty()) {
            _frames = decodeFrames(_maxFrames)
            invalidate()
        } else {

            for ((index, bitmap) in _frames.withIndex()) {
                val x: Float =
                    (index * _frameWH).toFloat() + _paddingLR
                canvas.drawBitmap(bitmap, x, dp(_paddingTB).toFloat(), null)
            }

        }

        val startX = (_timelineWidth * _progressStart) + _paddingLR
        val endX = (_timelineWidth * _progressEnd) + _paddingLR

        // top line
        canvas.drawRect(
            startX,
            dp(_paddingTB).toFloat(),
            endX,
            dp(_paddingTB).toFloat() - dp(_lineSize).toFloat(), _paint,
        )
        // bottom line
        canvas.drawRect(
            startX,
            _frameWH + dp(_paddingTB).toFloat(),
            endX,
            _frameWH + dp(_paddingTB) + dp(_lineSize).toFloat(), _paint,
        )


        // left line

        canvas.drawRect(
            startX,
            dp(_paddingTB - _lineSize).toFloat(),
            startX - dp(_lineSize).toFloat(),
            _frameWH + dp(_paddingTB + _lineSize).toFloat(), _paint,
        )

        // right line
        canvas.drawRect(
            endX,
            dp(_paddingTB - _lineSize).toFloat(),
            endX + dp(_lineSize).toFloat(),
            _frameWH + dp(_paddingTB + _lineSize).toFloat(), _paint,
        )



        _rect.set(
            startX - dp(8f),
            dp(_paddingTB - _lineSize).toFloat(),
            startX,
            _frameWH + dp(_paddingTB + _lineSize).toFloat()
        )
        canvas.drawRoundRect(_rect, dp(4f).toFloat(), dp(4f).toFloat(), _paint)


        _drawableLeft!!.setBounds(
            (startX - dp(8f)).toInt(), (height / 2) - dp(8f),
            startX.toInt(), (height / 2) + dp(8f)
        )
        _drawableLeft!!.draw(canvas)


        _rect.set(
            endX,
            dp(_paddingTB - _lineSize).toFloat(),
            endX + dp(8f),
            _frameWH + dp(_paddingTB + _lineSize).toFloat()
        )
        canvas.drawRoundRect(_rect, dp(4f).toFloat(), dp(4f).toFloat(), _paint)
        _drawableRight!!.setBounds(
            (endX).toInt(), (height / 2) - dp(8f),
            (endX + dp(8f)).toInt(), (height / 2) + dp(8f)
        )


        _drawableRight!!.draw(canvas)
        // Progress Indicator
        val indicatorPosition: Float = (_timelineWidth * _progress) + _paddingLR
        if (_playEnabled) {
            _rect.set(
                indicatorPosition - dp(1.5f),
                0f,
                indicatorPosition + dp(1.5f),
                height.toFloat()
            )
            // Progress Indicator Shadow Line
            canvas.drawRoundRect(
                _rect,
                dp(1f).toFloat(),
                dp(1f).toFloat(),
                _paintShadow
            )
            // Progress Indicator Shadow Circle
            canvas.drawCircle(
                indicatorPosition,
                height - dp(3f).toFloat(),
                dp(3.5f).toFloat(),
                _paintShadow
            )


            _rect.set(
                indicatorPosition - dp(1f),
                0f,
                indicatorPosition + dp(1f),
                height.toFloat()
            )
            // Progress Indicator Line
            canvas.drawRoundRect(
                _rect,
                dp(1f).toFloat(),
                dp(1f).toFloat(),
                _paint
            )
            // Progress Indicator Circle
            canvas.drawCircle(
                indicatorPosition,
                height - dp(3f).toFloat(),
                dp(3f).toFloat(),
                _paint
            )
        }




        if (_showTime) {
            _paintText.getTextBounds(durationText, 0, durationText.length, _rectCard)


            val textCenterX = (startX + endX) / 2


            val textBgXw = _rectCard.width() / 2
            val textBgStartX = textCenterX - textBgXw - dp(1f)
            val textBgEndX = textCenterX + textBgXw + dp(2f)


            val textBgYh = (_rectCard.height() / 2)
            val textBgStartY = (height.toFloat() / 2) - textBgYh - dp(4f)
            val textBgEndY = (height.toFloat() / 2) + textBgYh


            _rect.set(
                textBgStartX,
                textBgStartY,
                textBgEndX,
                textBgEndY
            )
            canvas.drawRoundRect(_rect, dp(2f).toFloat(), dp(2f).toFloat(), _paint)


            val x = (textCenterX - (_rectCard.width() / 2))
            val y = (height + _paintShadow.textSize) / 2
            canvas.drawText(durationText, x, y, _paintText)
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event == null) {
            return false
        }
        val x = event.x
        val y = event.y

        val width = measuredWidth - dp(32f)
        var startX = (width * _progressStart).toInt() + dp(16f)
        var playX: Int =
            (width * (_progressStart + (_progressEnd - _progressStart) * _progress)).toInt() + dp(
                16f
            )
        var endX = (width * _progressEnd).toInt() + dp(16f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val additionWidth = dp(12f)
                val additionWidthPlay = dp(8f)
                if (_playEnabled && playX - additionWidthPlay <= x && x <= playX + additionWidthPlay && y >= 0 && y <= measuredHeight) {
                    _pressedPlay = true
                    _listener?.onDraggingStateChanged(true)
                    _pressDx = (x - playX).toInt().toFloat()
                    invalidate()
                    return true
                } else if (startX - additionWidth <= x && x <= startX + additionWidth && y >= 0 && y <= measuredHeight) {
                    _pressedLeft = true
                    _listener?.onDraggingStateChanged(true)
                    _pressDx = (x - startX).toInt().toFloat()
                    invalidate()
                    return true
                } else if (endX - additionWidth <= x && x <= endX + additionWidth && y >= 0 && y <= measuredHeight) {
                    _pressedRight = true
                    _listener?.onDraggingStateChanged(true)
                    _pressDx = (x - endX).toInt().toFloat()
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (_pressedLeft) {
                    _pressedLeft = false
                    _listener?.onDraggingStateChanged(false)
                    return true
                } else if (_pressedRight) {
                    _pressedRight = false
                    _listener?.onDraggingStateChanged(false)
                    return true
                } else if (_pressedPlay) {
                    _pressedPlay = false
                    _listener?.onDraggingStateChanged(false)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (_pressedPlay && _playEnabled) {
                    playX = (x - _pressDx).toInt()
                    _progress = (playX - dp(16f)).toFloat() / width.toFloat()
                    if (_progress < _progressStart) {
                        _progress = _progressStart
                    } else if (_progress > _progressEnd) {
                        _progress = _progressEnd
                    }
                    _progress = (_progress - _progressStart) / (_progressEnd - _progressStart)

                    _listener?.onPlayProgressChanged(_progressStart + (_progressEnd - _progressStart) * _progress)

                    invalidate()
                    return true
                } else if (_pressedLeft) {
                    startX = (x - _pressDx).toInt()
                    if (startX < dp(16f)) {
                        startX = dp(16f)
                    } else if (startX > endX) {
                        startX = endX
                    }
                    _progressStart = (startX - dp(16f)).toFloat() / width.toFloat()
                    if (_progressEnd - _progressStart > _maxProgressDiff) {
                        _progressEnd = _progressStart + _maxProgressDiff
                    } else if (_minProgressDiff != 0f && _progressEnd - _progressStart < _minProgressDiff) {
                        _progressStart = _progressEnd - _minProgressDiff
                        if (_progressStart < 0) {
                            _progressStart = 0f
                        }
                    }

                    _listener?.onLeftProgressChanged(_progressStart)
                    _listener?.onDurationChanged(getCroppedDuration())

                    invalidate()
                    return true
                } else if (_pressedRight) {
                    endX = (x - _pressDx).toInt()
                    if (endX < startX) {
                        endX = startX
                    } else if (endX > width + dp(16f)) {
                        endX = width + dp(16f)
                    }
                    _progressEnd = (endX - dp(16f)).toFloat() / width.toFloat()
                    if (_progressEnd - _progressStart > _maxProgressDiff) {
                        _progressStart = _progressEnd - _maxProgressDiff
                    } else if (_minProgressDiff != 0f && _progressEnd - _progressStart < _minProgressDiff) {
                        _progressEnd = _progressStart + _minProgressDiff
                        if (_progressEnd > 1.0f) {
                            _progressEnd = 1.0f
                        }
                    }

                    _listener?.onRightProgressChanged(_progressEnd)
                    _listener?.onDurationChanged(getCroppedDuration())

                    invalidate()
                    return true
                }
            }
        }



        return false
    }

    private fun getCroppedDuration(): Long {
        val time: Float = _progressEnd - _progressStart
        return (_videoDuration * time).toLong()
    }
}