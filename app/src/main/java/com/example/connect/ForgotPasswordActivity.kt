package com.example.connect

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.connect.databinding.ForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow layout to draw behind status bar (for yellow header)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ViewBinding
        binding = ForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Button click
        binding.btnReset.setOnClickListener {
            sendResetLink()
        }
    }

    private fun sendResetLink() {
        val email = binding.etForgotEmail.text.toString().trim()

        // Validation
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return
        } else {
            binding.emailLayout.error = null
        }

        // Prevent multiple clicks
        binding.btnReset.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Password reset link sent. Check your email.",
                    Toast.LENGTH_LONG
                ).show()

                // Go back to Sign In screen
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnReset.isEnabled = true
                Toast.makeText(
                    this,
                    e.message ?: "Failed to send reset link",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
