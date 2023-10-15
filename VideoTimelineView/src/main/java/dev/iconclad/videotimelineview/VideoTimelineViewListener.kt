package dev.iconclad.videotimelineview

interface VideoTimelineViewListener {
    fun onLeftProgressChanged(progress: Float)
    fun onRightProgressChanged(progress: Float)
    fun onDurationChanged(duration: Long)
    fun onPlayProgressChanged(progress: Float)
    fun onDraggingStateChanged(isDragging: Boolean)
}