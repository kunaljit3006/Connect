package com.example.connect

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.connect.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SignIn : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToMain()
        }
    }

    private fun setupClickListeners() {
        binding.signInToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        binding.imageView2.setOnClickListener {
            signInUser()
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun signInUser() {
        val email = binding.editTextTextEmailAddress.text.toString().trim()
        val password = binding.editTextTextPassword3.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Enter a valid email"
            return
        } else binding.emailLayout.error = null

        if (password.isEmpty()) {
            binding.passwordLayout2.error = "Password required"
            return
        } else binding.passwordLayout2.error = null

        binding.imageView2.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                saveUserToFirestore()
            }
            .addOnFailureListener {
                binding.imageView2.isEnabled = true
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserToFirestore() {
        val user = auth.currentUser ?: return

        val userMap = hashMapOf(
            "uid" to user.uid,
            "email" to user.email,
            "username" to "kunaljit kashyap",
            "profileUrl" to ""
        )

        db.collection("users")
            .document(user.uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                navigateToMain()
            }
            .addOnFailureListener {
                binding.imageView2.isEnabled = true
                Toast.makeText(this, "Login successful, but data sync failed", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
