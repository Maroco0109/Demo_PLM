
package com.maroco.demo_plm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import ai.onnxruntime.OrtEnvironment
import android.util.Log

class SmsFragment : Fragment() {

    private lateinit var recyclerViewSms: RecyclerView
    private lateinit var tabLayout: TabLayout
    private val inboxList = mutableListOf<Pair<String, Boolean>>()
    private val spamList = mutableListOf<Pair<String, Boolean>>()

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

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                recyclerViewSms.adapter = when (tab.position) {
                    0 -> SmsAdapter(spamList)
                    1 -> SmsAdapter(inboxList)
                    else -> null
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_SMS),
                101
            )
        } else {
            loadSmsMessages()
        }
    }

    private fun loadSmsMessages() {
        val smsUri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.BODY)
        val cursor = requireContext().contentResolver.query(smsUri, projection, null, null, null)

        val ortEnv = OrtEnvironment.getEnvironment()
        val sessionKoBert = (activity as MainActivity).sessionKoBert
        val sessionKoElectra = (activity as MainActivity).sessionKoElectra
        val sessionKoRoberta = (activity as MainActivity).sessionKoRoberta

        val tokenizerBert = Tokenizer(requireContext(), "vocab_kobert.txt", "tokenizer_config_kobert.json", subwordPrefix = "##")
        val tokenizerElectra = Tokenizer(requireContext(), "vocab_koelectra.txt", "tokenizer_config_koelectra.json", subwordPrefix = "##")
        val tokenizerRoberta = Tokenizer(requireContext(), "vocab_koroberta.txt", "tokenizer_config_koroberta.json", subwordPrefix = "Ġ")

        val classifierBert = ModelClassifier(ortEnv, tokenizerBert, sessionKoBert)
        val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, sessionKoElectra)
        val classifierRoberta = ModelClassifier(ortEnv, tokenizerRoberta, sessionKoRoberta)

        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)

                val (spam1, _) = classifierBert.classify(body)
                val (spam2, _) = classifierElectra.classify(body)
                val (spam3, _) = classifierRoberta.classify(body)

                // Soft Voting: 두 모델 스팸 확률 평균으로 판정
                val avgSpam = (spam1 + spam2 + spam3) / 3
                val isSpam  = avgSpam >= 60.0f

                Log.d("ModelBERT", "Spam: $spam1")
                Log.d("ModelELECTRA", "Spam: $spam2")
                Log.d("ModelROBERTA", "Spam: $spam3")

                val pair = Pair(body, isSpam)
                if (isSpam) spamList.add(pair) else inboxList.add(pair)
            }
        }

        // 기본 탭은 Inbox
        recyclerViewSms.adapter = SmsAdapter(inboxList)
    }
}
