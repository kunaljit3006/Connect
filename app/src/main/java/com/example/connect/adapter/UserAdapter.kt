package com.example.connect.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.connect.R
import com.example.connect.databinding.ItemUserBinding
import com.example.connect.model.User

class UserAdapter(
    private val userList: ArrayList<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val filteredList: ArrayList<User> = ArrayList()

    inner class UserViewHolder(
        val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = filteredList[position]

        holder.binding.apply {
            textUserName.text = user.name
            textStatus.text = user.status

            // Color the status text: red for Offline, green for Online
            val statusColor = if (user.status == "Online") {
                androidx.core.content.ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
            } else {
                androidx.core.content.ContextCompat.getColor(root.context, android.R.color.holo_red_dark)
            }
            textStatus.setTextColor(statusColor)

            Glide.with(imageProfile.context)
                .load(user.profileUrl)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(imageProfile)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    // Update full list using DiffUtil for efficient, animated updates
    fun updateList(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        applyFilter(filteredList.let { current ->
            // Determine the current active filter by checking if filtered differs from full
            if (current.size == userList.size) "" else null
        })
        dispatchDiff(filteredList, ArrayList(userList))
    }

    // Search filter using DiffUtil
    fun filter(query: String) {
        val newFiltered = if (query.isEmpty()) {
            ArrayList(userList)
        } else {
            ArrayList(userList.filter {
                it.name.contains(query, ignoreCase = true)
            })
        }
        dispatchDiff(filteredList, newFiltered)
    }

    private fun applyFilter(query: String?) {
        val newFiltered = if (query.isNullOrEmpty()) {
            ArrayList(userList)
        } else {
            ArrayList(userList.filter {
                it.name.contains(query, ignoreCase = true)
            })
        }
        dispatchDiff(filteredList, newFiltered)
    }

    private fun dispatchDiff(oldList: List<User>, newList: List<User>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                // Use uid to identify same user across updates
                return oldList[oldPos].uid == newList[newPos].uid
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return oldList[oldPos] == newList[newPos]
            }
        })

        filteredList.clear()
        filteredList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}
