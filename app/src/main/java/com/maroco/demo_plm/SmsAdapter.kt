package com.maroco.demo_plm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SmsAdapter(private val smsList: List<Pair<String, Boolean>>) :
    RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {

    class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val smsTypeText: TextView = itemView.findViewById(R.id.smsTypeText)
        val smsBodyText: TextView = itemView.findViewById(R.id.smsBodyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val (message, isSpam) = smsList[position]
        holder.smsTypeText.text = if (isSpam) "Spam" else "Inbox"
        holder.smsBodyText.text = message
    }

    override fun getItemCount(): Int = smsList.size
}