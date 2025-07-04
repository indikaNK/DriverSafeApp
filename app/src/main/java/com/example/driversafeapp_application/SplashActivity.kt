package com.example.driversafeapp_application



import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.splashVideoView)
        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.intro_video) // Replace with your video file name
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = false // Prevent looping
            videoView.start()
        }

        videoView.setOnCompletionListener {
            // Transition to MainActivity after video ends (5 seconds)
            startMainActivity()
        }

        // Fallback: Transition after 5 seconds if video ends prematurely
        Handler(Looper.getMainLooper()).postDelayed({
            if (videoView.isPlaying) {
                videoView.stopPlayback()
            }
            startMainActivity()
        }, 5000) // 5 seconds
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close SplashActivity
    }

    override fun onDestroy() {
        super.onDestroy()
        val videoView = findViewById<VideoView>(R.id.splashVideoView)
        videoView?.stopPlayback()
    }
}