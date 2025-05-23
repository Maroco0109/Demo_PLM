// SmsFragment.kt
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
import java.io.File
import java.io.FileWriter

class SmsFragment : Fragment() {
    private var logWriter: FileWriter? = null
    private lateinit var recyclerViewSms: RecyclerView
    private lateinit var tabLayout: TabLayout

    private val ortEnv by lazy { OrtEnvironment.getEnvironment() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_sms, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewSms = view.findViewById(R.id.recyclerViewSms)
        tabLayout       = view.findViewById(R.id.tabLayout)
        recyclerViewSms.layoutManager = LinearLayoutManager(requireContext())

        // 토크나이저 & 분류기
        val tokenizerBert    = Tokenizer(requireContext(),
            "vocab_kobert.txt",    "tokenizer_config_kobert.json",    subwordPrefix="##")
        val tokenizerElectra = Tokenizer(requireContext(),
            "vocab_koelectra.txt","tokenizer_config_koelectra.json", subwordPrefix="##")
        val tokenizerRoberta = Tokenizer(requireContext(),
            "vocab_koroberta.txt", "tokenizer_config_koroberta.json", subwordPrefix="Ġ")

        val classifierBert    = ModelClassifier(ortEnv, tokenizerBert,    ModelManager.sessionKoBert)
        val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, ModelManager.sessionKoElectra)
        val classifierRoberta = ModelClassifier(ortEnv, tokenizerRoberta, ModelManager.sessionKoRoberta)

        // 로그 파일 초기화
        val logDir = File(requireContext().filesDir, "main/logs")
        if (!logDir.exists()) logDir.mkdirs()
        logWriter = FileWriter(File(logDir, "sms_log.txt"), true).apply {
            write("=== SMS Tokenization Log: ${System.currentTimeMillis()} ===\n\n")
            flush()
        }

        // 탭 구성
        tabLayout.addTab(tabLayout.newTab().setText("Inbox"))
        tabLayout.addTab(tabLayout.newTab().setText("Spam"))

        fun logAndDisplay(list: List<Pair<String, Boolean>>) {
            list.forEachIndexed { idx, (text, _) ->
                val (idsB, _, _) = tokenizerBert.tokenize(text)
                val (idsE, _, _) = tokenizerElectra.tokenize(text)
                val (idsR, _, _) = tokenizerRoberta.tokenize(text)
                logWriter?.apply {
                    write("[${idx+1}] $text\n")
                    write("BERT IDs:    ${idsB.joinToString(" ")}\n")
                    write("ELECTRA IDs: ${idsE.joinToString(" ")}\n")
                    write("ROBERTA IDs: ${idsR.joinToString(" ")}\n\n")
                    flush()
                }
            }
            recyclerViewSms.adapter = SmsAdapter(list)
        }

        // 권한 확인 & 요청
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_SMS),
                101  // literal로 대체
            )
        } else {
            logAndDisplay(MainActivity.inboxMessages)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val list = if (tab.position == 0)
                    MainActivity.inboxMessages
                else
                    MainActivity.spamMessages
                logAndDisplay(list)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logWriter?.close()
    }
}
