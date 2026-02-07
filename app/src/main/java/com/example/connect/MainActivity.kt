package com.example.connect

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.connect.adapter.UserAdapter
import com.example.connect.databinding.ActivityMainBinding
import com.example.connect.databinding.NavHeaderBinding
import com.example.connect.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var headerBinding: NavHeaderBinding
    private lateinit var adapter: UserAdapter
    private val userList = ArrayList<User>()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var headerListener: ListenerRegistration? = null

    private val DUMMY_PROFILE_URL =
        "https://drive.google.com/uc?export=view&id=1Uy0Do0ASVDWjriEZbeVWsPBv_ToXrjE-"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout safely
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply system insets safely
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            insets
        }

        // Initialize navigation header safely (important for dark mode recreation)
        setupNavigationHeader()

        setupDrawer()
        forceDeleteTextRed()
        setupRecyclerView()
        setupSearchBar()
    }

    /**
     * Navigation header must be added only once.
     * Activity recreates on dark/light mode change.
     */
    private fun setupNavigationHeader() {
        headerBinding =
            if (binding.navigationView.headerCount == 0) {
                val headerView = layoutInflater.inflate(
                    R.layout.nav_header,
                    binding.navigationView,
                    false
                )
                binding.navigationView.addHeaderView(headerView)
                NavHeaderBinding.bind(headerView)
            } else {
                NavHeaderBinding.bind(binding.navigationView.getHeaderView(0))
            }

        loadCurrentUserIntoHeader()
    }

    /**
     * Loads current logged-in user into nav header.
     * Uses UID as document ID (cross-device safe).
     */
    private fun loadCurrentUserIntoHeader() {
        val uid = auth.currentUser?.uid ?: return

        // Remove old listener if activity recreated
        headerListener?.remove()

        headerListener = db.collection("users")
            .document(uid)
            .addSnapshotListener { document, error ->
                if (error != null || document == null || !document.exists()) return@addSnapshotListener

                headerBinding.txtUsername.text =
                    document.getString("username") ?: "User"

                headerBinding.txtEmail.text =
                    document.getString("email") ?: ""

                val profileUrl = document.getString("profileUrl")

                Glide.with(this)
                    .load(if (!profileUrl.isNullOrEmpty()) profileUrl else DUMMY_PROFILE_URL)
                    .placeholder(R.drawable.user)
                    .into(headerBinding.profileImage)
            }
    }

    /**
     * Drawer open/close and menu handling
     */
    private fun setupDrawer() {
        binding.imageView4.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    // Open profile screen later
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                }
                R.id.nav_delete -> {
                    // Handle delete later
                    showDeleteAccountDialog()
                }
                R.id.nav_report_bug->{
                    openReportAbug()
                }
                R.id.nav_about_developer->{
                    openAboutDeveloper()
                }

            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    /**
     * Online / Offline status handling
     */
    override fun onResume() {
        super.onResume()
        updateStatus("Online")
    }

    override fun onPause() {
        super.onPause()
        updateStatus("Offline")
    }

    private fun updateStatus(status: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("status", status)
    }

    /**
     * RecyclerView setup
     */
    private fun setupRecyclerView() {
        adapter = UserAdapter(userList)

        binding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }

        fetchUsersFromFirestore()
    }

    /**
     * Fetch all users (works independently of header)
     */
    private fun fetchUsersFromFirestore() {
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val tempList = ArrayList<User>()
                for (doc in snapshot.documents) {
                    tempList.add(
                        User(
                            name = doc.getString("username") ?: "Unknown",
                            status = doc.getString("status") ?: "Offline",
                            profileUrl = DUMMY_PROFILE_URL
                        )
                    )
                }
                adapter.updateList(tempList)
            }
    }

    /**
     * Search bar filtering
     */
    private fun setupSearchBar() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
        })
    }

    /**
     * Clean up Firestore listeners
     */
    override fun onDestroy() {
        super.onDestroy()
        headerListener?.remove()
    }

    private fun forceDeleteTextRed() {
        val item = binding.navigationView.menu.findItem(R.id.nav_delete) ?: return

        val red = getColor(android.R.color.holo_red_dark)
        val title = android.text.SpannableString(item.title)
        title.setSpan(
            android.text.style.ForegroundColorSpan(red),
            0,
            title.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        item.title = title
    }
    private fun openAboutDeveloper() {
        val intent = Intent(this, AboutTheDevloperActivity::class.java)
        startActivity(intent)
    }
    private  fun  openReportAbug(){
        val intent= Intent(this, ReportABugActivity::class.java)
        startActivity(intent)
    }


    private fun showLogoutDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Log out?")
            .setMessage("You will need to log in again to use the app.")
            .setCancelable(true)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out") { _, _ ->
                performLogout()
            }
            .create()

        dialog.show()

        // Make only the "Log out" button red
        val redColor = getColor(android.R.color.holo_red_dark)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(redColor)
    }
    private fun performLogout() {
        headerListener?.remove()
        headerListener = null

        auth.signOut()

        val intent = Intent(this, SignIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showDeleteAccountDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Delete account?")
            .setMessage(
                "This action is permanent.\n" +
                        "All your data will be deleted and cannot be recovered."
            )
            .setCancelable(false)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                performDeleteAccount()
            }
            .create()

        dialog.show()

        // Make ONLY the "Delete" button red
        val redColor = getColor(android.R.color.holo_red_dark)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(redColor)
    }
    private fun performDeleteAccount() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Stop listeners
        headerListener?.remove()
        headerListener = null

        // 1️ Delete Firestore user data FIRST
        db.collection("users")
            .document(uid)
            .delete()
            .addOnSuccessListener {

                // 2️ Delete Firebase Auth account
                user.delete()
                    .addOnSuccessListener {

                        // 3️ Go to SignUp (clear back stack)
                        val intent = Intent(this, SignUp::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        // Usually happens if re-authentication is required
                    }
            }
            .addOnFailureListener {
                // Firestore delete failed
            }
    }


}


