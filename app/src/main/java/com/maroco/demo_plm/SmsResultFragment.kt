package com.maroco.demo_plm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SmsResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sms_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutSms)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerSms)

        val inboxList = arguments?.getStringArrayList("inbox") ?: arrayListOf()
        val spamList = arguments?.getStringArrayList("spam") ?: arrayListOf()

        viewPager.adapter = SmsResultPageAdapter(this, inboxList, spamList)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Inbox"
                1 -> "Spam"
                else -> ""
            }
        }.attach()
    }
}