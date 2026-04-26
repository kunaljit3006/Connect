package com.example.connect

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class ReportABugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_report_a_bug)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val mainView = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            
            // Pad sides and bottom only
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            
            // Pad header top so content moves safely below camera cutout
            val header = findViewById<android.view.View>(R.id.headerLayout)
            val density = resources.displayMetrics.density
            val pad16 = (16 * density).toInt()
            val paddingTop = if (systemBars.top > 0) systemBars.top + pad16 else pad16 + 50 // fallback for notch
            header.setPadding(pad16, paddingTop, pad16, pad16)
            
            insets
        }
    }
}