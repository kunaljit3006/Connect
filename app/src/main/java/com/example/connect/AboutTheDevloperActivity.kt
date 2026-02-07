package com.example.connect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.example.connect.databinding.ActivityAboutTheDevloperBinding

class AboutTheDevloperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutTheDevloperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ViewBinding
        binding = ActivityAboutTheDevloperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Load profile image from Google Drive
        loadProfileImageFromDrive()

        // Setup social media click actions
        setupSocialMediaClicks()
    }

    private fun loadProfileImageFromDrive() {
        val imageUrl =
            "https://drive.google.com/uc?export=view&id=1Uy0Do0ASVDWjriEZbeVWsPBv_ToXrjE-"

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.profile_pic_icon)
            .error(R.drawable.profile_pic_icon)
            .circleCrop()
            .into(binding.profile)
    }

    private fun setupSocialMediaClicks() {

        // GitHub (browser is best)
        binding.ivGithub.setOnClickListener {
            openLink("https://github.com/kunaljit3006")
        }



        // LinkedIn — app first, browser fallback
        binding.ivLinkedIn.setOnClickListener {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("linkedin://in/kunaljit-kashyap-8bb212287")
                )
                startActivity(intent)
            } catch (e: Exception) {
                openLink("https://www.linkedin.com/in/kunaljit-kashyap-8bb212287/")
            }
        }



        // Gmail — open mail app directly
        binding.ivGmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:kkunaljit@gmail.com")
            }
            startActivity(intent)
        }
    }


    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
