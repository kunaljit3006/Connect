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
import com.example.connect.adapter.UserActionCallback
import com.example.connect.databinding.ActivityMainBinding
import com.example.connect.databinding.NavHeaderBinding
import com.example.connect.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var headerBinding: NavHeaderBinding
    private lateinit var adapter: UserAdapter
    private val userList = ArrayList<User>()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var headerListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var friendshipsListener: ListenerRegistration? = null
    private var incomingCallListener: ListenerRegistration? = null

    private var rawUsers = ArrayList<User>()
    private var friendshipMap = mutableMapOf<String, Triple<String, String, Long>>() // uid -> (docId, status, lastInteraction)

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
        listenForIncomingCalls()
        
        binding.btnFriendRequests.setOnClickListener {
            startActivity(Intent(this, FriendRequestsActivity::class.java))
        }
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

        headerBinding.root.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
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
                    .load(profileUrl.takeIf { !it.isNullOrEmpty() })
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
                    startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
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
        adapter = UserAdapter(userList, object : UserActionCallback {
            override fun onAddFriendClicked(user: User) {
                sendFriendRequest(user.uid)
            }

            override fun onCallClicked(user: User) {
                if (user.friendStatus != "friends") {
                    android.widget.Toast.makeText(this@MainActivity, "You can only call your friends.", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                initiateCall(user)
            }

            override fun onUserLongClicked(user: User) {
                showUserOptionsBottomSheet(user)
            }
        })

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
        val currentUid = auth.currentUser?.uid ?: return

        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val tempList = ArrayList<User>()
                for (doc in snapshot.documents) {
                    if (doc.id == currentUid) continue

                    val rawUrl = doc.getString("profileUrl")
                    tempList.add(
                        User(
                            uid = doc.id,
                            name = doc.getString("username") ?: "Unknown",
                            status = doc.getString("status") ?: "Offline",
                            profileUrl = rawUrl ?: ""
                        )
                    )
                }
                rawUsers = tempList
                mergeAndDisplayUsers()
            }

        friendshipsListener = db.collection("friendships")
            .whereArrayContains("users", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                var pendingCount = 0
                val newMap = mutableMapOf<String, Triple<String, String, Long>>()
                for (doc in snapshot.documents) {
                    val usersArr = doc.get("users") as? List<String>
                    if (usersArr == null || usersArr.size != 2) continue
                    
                    val otherUid = usersArr.firstOrNull { it != currentUid } ?: continue
                    val senderId = doc.getString("senderId") ?: ""
                    val status = doc.getString("status") ?: "pending"
                    val lastInteraction = doc.getLong("lastInteraction") ?: 0L

                    if (status == "pending" && senderId != currentUid) {
                        pendingCount++
                    }

                    val uiStatus = when (status) {
                        "accepted" -> "friends"
                        "blocked" -> if (senderId == currentUid) "blocked_by_me" else "blocked_by_them"
                        else -> if (senderId == currentUid) "sent" else "received"
                    }

                    newMap[otherUid] = Triple(doc.id, uiStatus, lastInteraction)
                }
                friendshipMap = newMap
                mergeAndDisplayUsers()
                updateFriendRequestBadge(pendingCount)
            }
    }

    private fun mergeAndDisplayUsers() {
        val mergedList = rawUsers.mapNotNull { user ->
            val friendData = friendshipMap[user.uid]
            val fStatus = friendData?.second ?: "none"
            
            // Only show users if they haven't blocked current user
            if (fStatus == "blocked_by_them") {
                null
            } else {
                user.copy(
                    friendStatus = fStatus,
                    friendshipDocId = friendData?.first,
                    lastInteraction = friendData?.third ?: 0L
                )
            }
        }.sortedByDescending { it.lastInteraction }
        
        adapter.updateList(mergedList)
    }

    private fun updateFriendRequestBadge(count: Int) {
        val badge = findViewById<TextView>(R.id.tvFriendRequestBadge) ?: return
        if (count > 0) {
            badge.visibility = android.view.View.VISIBLE
            badge.text = if (count > 99) "99+" else count.toString()
        } else {
            badge.visibility = android.view.View.GONE
        }
    }

    private fun getFriendshipDocId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    private fun sendFriendRequest(otherUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val docId = getFriendshipDocId(currentUid, otherUid)

        val friendshipData = hashMapOf(
            "users" to listOf(currentUid, otherUid),
            "senderId" to currentUid,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("friendships").document(docId).set(friendshipData)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Friend request sent!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(this, "Failed to send: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }

    private fun showUserOptionsBottomSheet(user: User) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_user_actions, null)
        bottomSheetDialog.setContentView(view)

        val tvUsername = view.findViewById<TextView>(R.id.tvSheetUsername)
        val tvUnfriend = view.findViewById<TextView>(R.id.tvUnfriend)
        val tvBlock = view.findViewById<TextView>(R.id.tvBlock)
        val tvUnblock = view.findViewById<TextView>(R.id.tvUnblock)

        tvUsername.text = user.name

        // Show/Hide options based on friendship status
        if (user.friendStatus == "friends") {
            tvUnfriend.visibility = android.view.View.VISIBLE
        } else {
            tvUnfriend.visibility = android.view.View.GONE
        }

        if (user.friendStatus == "blocked_by_me") {
            tvBlock.visibility = android.view.View.GONE
            tvUnblock.visibility = android.view.View.VISIBLE
        } else {
            tvBlock.visibility = android.view.View.VISIBLE
            tvUnblock.visibility = android.view.View.GONE
        }

        tvUnfriend.setOnClickListener {
            bottomSheetDialog.dismiss()
            showConfirmDialog("Unfriend", "Are you sure you want to unfriend ${user.name}?") {
                unfriendUser(user)
            }
        }

        tvBlock.setOnClickListener {
            bottomSheetDialog.dismiss()
            showConfirmDialog("Block User", "Are you sure you want to block ${user.name}? They won't be able to find you.", isDestructive = true) {
                blockUser(user)
            }
        }

        tvUnblock.setOnClickListener {
            bottomSheetDialog.dismiss()
            showConfirmDialog("Unblock User", "Are you sure you want to unblock ${user.name}?") {
                unblockUser(user)
            }
        }

        bottomSheetDialog.show()
    }

    private fun showConfirmDialog(title: String, message: String, isDestructive: Boolean = false, onConfirm: () -> Unit) {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .create()

        dialog.show()

        if (isDestructive) {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun unfriendUser(user: User) {
        val docId = user.friendshipDocId ?: getFriendshipDocId(auth.currentUser?.uid ?: return, user.uid)
        db.collection("friendships").document(docId).delete()
            .addOnSuccessListener { android.widget.Toast.makeText(this, "Unfriended ${user.name}", android.widget.Toast.LENGTH_SHORT).show() }
    }

    private fun blockUser(user: User) {
        val currentUid = auth.currentUser?.uid ?: return
        val docId = getFriendshipDocId(currentUid, user.uid)

        val blockData = hashMapOf(
            "users" to listOf(currentUid, user.uid),
            "senderId" to currentUid,
            "status" to "blocked",
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("friendships").document(docId).set(blockData)
            .addOnSuccessListener { android.widget.Toast.makeText(this, "Blocked ${user.name}", android.widget.Toast.LENGTH_SHORT).show() }
    }

    private fun unblockUser(user: User) {
        val docId = user.friendshipDocId ?: return
        db.collection("friendships").document(docId).delete()
            .addOnSuccessListener { android.widget.Toast.makeText(this, "Unblocked ${user.name}", android.widget.Toast.LENGTH_SHORT).show() }
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
        usersListener?.remove()
        friendshipsListener?.remove()
        incomingCallListener?.remove()
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

    private fun listenForIncomingCalls() {
        val currentUid = auth.currentUser?.uid ?: return
        incomingCallListener = db.collection("calls")
            .whereEqualTo("receiverId", currentUid)
            .whereEqualTo("status", "dialing")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                for (doc in snapshot.documentChanges) {
                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val callId = doc.document.id
                        val callerId = doc.document.getString("callerId") ?: continue
                        val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                        
                        // Ignore calls older than 60 seconds
                        if (System.currentTimeMillis() - timestamp > 60000) {
                            db.collection("calls").document(callId).update("status", "ended")
                            continue
                        }
                        
                        val callerUser = rawUsers.find { it.uid == callerId }
                        val callerName = callerUser?.name ?: "Incoming Call"
                        val callerProfileUrl = callerUser?.profileUrl ?: ""

                        val intent = Intent(this, IncomingCallActivity::class.java).apply {
                            putExtra("callId", callId)
                            putExtra("callerName", callerName)
                            putExtra("profileUrl", callerProfileUrl)
                        }
                        startActivity(intent)
                    }
                }
            }
    }

    private fun initiateCall(user: User) {
        val currentUid = auth.currentUser?.uid ?: return
        val callId = db.collection("calls").document().id
        val callData = hashMapOf(
            "callerId" to currentUid,
            "receiverId" to user.uid,
            "status" to "dialing",
            "timestamp" to System.currentTimeMillis()
        )
        android.widget.Toast.makeText(this, "Starting call...", android.widget.Toast.LENGTH_SHORT).show()
        db.collection("calls").document(callId).set(callData)
            .addOnSuccessListener {
                // Update last interaction timestamp to bring this user to top
                user.friendshipDocId?.let { docId ->
                    db.collection("friendships").document(docId)
                        .update("lastInteraction", System.currentTimeMillis())
                }

                val intent = Intent(this, CallActivity::class.java).apply {
                    putExtra("callId", callId)
                    putExtra("targetName", user.name)
                    putExtra("profileUrl", user.profileUrl)
                    putExtra("isCaller", true)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(this, "Failed to call: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
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

        val uid = auth.currentUser?.uid

        // Set Offline BEFORE signing out — after signOut(), currentUser becomes null
        if (uid != null) {
            db.collection("users").document(uid)
                .update("status", "Offline")
                .addOnCompleteListener {
                    // Sign out and navigate only after status is updated
                    auth.signOut()
                    val intent = Intent(this, SignIn::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
        } else {
            auth.signOut()
            val intent = Intent(this, SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
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
                    .addOnFailureListener { e ->
                        // Firebase requires recent login before deleting account
                        // Show a clear message so the user knows what to do
                        android.widget.Toast.makeText(
                            this,
                            "For security, please sign out and sign in again, then try deleting your account.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(
                    this,
                    "Failed to delete account data. Please try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
    }


}


