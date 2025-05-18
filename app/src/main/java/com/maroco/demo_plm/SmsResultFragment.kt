package com.maroco.demo_plm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SmsResultFragment : Fragment() {

    private lateinit var recyclerViewInbox: RecyclerView
    private lateinit var recyclerViewSpam: RecyclerView
    private lateinit var inboxAdapter: SmsAdapter
    private lateinit var spamAdapter: SmsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sms, container, false)

        recyclerViewInbox = view.findViewById(R.id.recyclerViewInbox)
        recyclerViewSpam = view.findViewById(R.id.recyclerViewSpam)

        recyclerViewInbox.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewSpam.layoutManager = LinearLayoutManager(requireContext())

        val inboxList = SplashActivity.inboxMessages.map { it.first().toString() }
        val spamList = SplashActivity.spamMessages.map { it.first().toString() }

        inboxAdapter = SmsAdapter(inboxList)
        spamAdapter = SmsAdapter(spamList)

        recyclerViewInbox.adapter = inboxAdapter
        recyclerViewSpam.adapter = spamAdapter

        val inboxHeader: TextView = view.findViewById(R.id.inboxHeader)
        val spamHeader: TextView = view.findViewById(R.id.spamHeader)

        inboxHeader.text = "Inbox (${inboxList.size})"
        spamHeader.text = "Spam (${spamList.size})"

        return view
    }
}