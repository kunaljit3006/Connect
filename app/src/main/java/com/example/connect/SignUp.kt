package com.example.connect

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.connect.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class SignUp :  AppCompatActivity(){

    private lateinit var binding: ActivitySignUpBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        binding.tvAlreadyAccount.setOnClickListener {
            goToSignIn()
        }

        binding.button.setOnClickListener {
            signUpUser()
        }
    }

    private fun signUpUser() {

        val username = binding.editTextUsername.text.toString().trim()
        val email = binding.editTextTextEmailAddress.text.toString().trim()
        val password = binding.editTextTextPassword2.text.toString()
        val confirmPassword = binding.editTextTextPassword3.text.toString()

        // ---------- VALIDATION ----------
        if (username.isEmpty()) {
            binding.usernameLayout.error = "Username required"
            return
        } else binding.usernameLayout.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Valid email required"
            return
        } else binding.emailLayout.error = null

        if (password.length < 6) {
            binding.passwordLayout.error = "Minimum 6 characters"
            return
        } else binding.passwordLayout.error = null

        if (password != confirmPassword) {
            binding.passwordLayout2.error = "Passwords do not match"
            return
        } else binding.passwordLayout2.error = null

        // ---------- CHECK USERNAME UNIQUENESS ----------
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    binding.usernameLayout.error = "Username already taken"
                } else {
                    createUser(email, password, username)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUser(email: String, password: String, username: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user ?: return@addOnSuccessListener
                val uid = user.uid

                // Firestore document ID = UID (CORRECT)
                val userMap = hashMapOf(
                    "uid" to uid,
                    "username" to username,
                    "email" to email,
                    "status" to "Offline",
                    "profileUrl" to ""
                )

                db.collection("users")
                    .document(uid)
                    .set(userMap)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Account created successfully. Please sign in.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Sign out after signup (correct for your flow)
                        auth.signOut()
                        goToSignIn()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to save user data",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthUserCollisionException) {
                    Toast.makeText(
                        this,
                        "Account already exists. Please sign in.",
                        Toast.LENGTH_LONG
                    ).show()
                    goToSignIn()
                } else {
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun goToSignIn() {
        startActivity(Intent(this, SignIn::class.java))
        finish()
    }
}
