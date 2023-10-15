package dev.iconclad.videotimelineview.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dev.iconclad.videotimelineview.VideoTimelineView
import dev.iconclad.videotimelineview.VideoTimelineViewListener

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val videoTimelineView = findViewById<VideoTimelineView>(R.id.vtv)
       // videoTimelineView.setVideoPath("")
        videoTimelineView.setProgress(0.1f)
        videoTimelineView.setListener(object : VideoTimelineViewListener{
            override fun onLeftProgressChanged(progress: Float) {

            }

            override fun onRightProgressChanged(progress: Float) {

            }

            override fun onDurationChanged(duration: Long) {

            }

            override fun onPlayProgressChanged(progress: Float) {

            }

            override fun onDraggingStateChanged(isDragging: Boolean) {

            }
        })
    }
}