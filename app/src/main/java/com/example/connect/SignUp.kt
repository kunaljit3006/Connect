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

class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Do NOT apply top padding to root so background draws under status bar
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)

            // Shift animation down below status bar
            val lottieLp = binding.lottieAnimationView2.layoutParams as android.view.ViewGroup.MarginLayoutParams
            lottieLp.topMargin = (16 * resources.displayMetrics.density).toInt() + systemBars.top
            binding.lottieAnimationView2.layoutParams = lottieLp
            
            // Expand yellow background height (280dp in SignUp)
            val bgLp = binding.topBackground.layoutParams
            bgLp.height = (280 * resources.displayMetrics.density).toInt() + systemBars.top
            binding.topBackground.layoutParams = bgLp

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

        binding.button.isEnabled = false

        // Create Auth account first — user is then authenticated for the Firestore query
        createUser(email, password, username)
    }

    private fun createUser(email: String, password: String, username: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user ?: return@addOnSuccessListener
                val uid = user.uid

                // Now authenticated — safe to query Firestore for username uniqueness
                db.collection("users")
                    .whereEqualTo("username", username)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { docs ->
                        if (!docs.isEmpty) {
                            // Username taken — delete the just-created Auth account to keep things clean
                            user.delete()
                            binding.usernameLayout.error = "Username already taken"
                            binding.button.isEnabled = true
                        } else {
                            saveUserToFirestore(uid, email, username)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Firestore check failed — clean up Auth account
                        user.delete()
                        binding.button.isEnabled = true
                        Toast.makeText(this, "Sign up failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.button.isEnabled = true
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

    private fun saveUserToFirestore(uid: String, email: String, username: String) {
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

                auth.signOut()
                goToSignIn()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to save user data",
                    Toast.LENGTH_LONG
                ).show()
                binding.button.isEnabled = true
            }
    }

    private fun goToSignIn() {
        startActivity(Intent(this, SignIn::class.java))
        finish()
    }
}
