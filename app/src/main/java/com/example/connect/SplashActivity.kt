package com.example.connect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    // Store handler + runnable so we can cancel them if the activity is destroyed
    private val splashHandler = Handler(Looper.getMainLooper())
    private val splashRunnable = Runnable {
        val user = auth.currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, SignIn::class.java))
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        splashHandler.postDelayed(splashRunnable, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel pending navigation if the activity is destroyed before the delay fires
        splashHandler.removeCallbacks(splashRunnable)
    }
}

