package com.example.connect.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
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

            Glide.with(imageProfile.context)
                .load(user.profileUrl)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(imageProfile)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    // 🔥 IMPORTANT: call this when Firestore data changes
    fun updateList(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)

        filteredList.clear()
        filteredList.addAll(newList)

        notifyDataSetChanged()
    }

    // 🔍 Search filter
    fun filter(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(userList)
        } else {
            filteredList.addAll(
                userList.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            )
        }
        notifyDataSetChanged()
    }
}
