package com.maroco.demo_plm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.maroco.demo_plm.MainActivity
import com.maroco.demo_plm.SmsAdapter

class SmsFragment : Fragment() {

    private lateinit var recyclerViewSms: RecyclerView
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewSms = view.findViewById(R.id.recyclerViewSms)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerViewSms.layoutManager = LinearLayoutManager(requireContext())

        tabLayout.addTab(tabLayout.newTab().setText("Inbox"))
        tabLayout.addTab(tabLayout.newTab().setText("Spam"))

        // 탭 선택 시 이미 분류된 결과만 표시
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                recyclerViewSms.adapter = when (tab.position) {
                    0 -> SmsAdapter(MainActivity.inboxMessages)
                    1 -> SmsAdapter(MainActivity.spamMessages)
                    else -> SmsAdapter(emptyList())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 초기 탭: Inbox
        recyclerViewSms.adapter = SmsAdapter(MainActivity.inboxMessages)
    }
}