package com.maroco.demo_plm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maroco.demo_plm.R

class SmsAdapter(private val smsList: List<Pair<String, Boolean>>) :
    RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {

    class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeText: TextView = itemView.findViewById(R.id.smsTypeText)
        val contentText: TextView = itemView.findViewById(R.id.smsBodyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val (message, isSpam) = smsList[position]
        holder.typeText.text = if (isSpam) "Spam" else "Inbox"
        holder.contentText.text = message
    }

    override fun getItemCount(): Int = smsList.size
}