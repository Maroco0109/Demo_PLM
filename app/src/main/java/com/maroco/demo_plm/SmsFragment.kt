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
import ai.onnxruntime.OrtEnvironment

class SmsFragment : Fragment() {

    private lateinit var recyclerViewSms: RecyclerView
    private val smsList = mutableListOf<Pair<String, Boolean>>() // (text, isSpam)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewSms = view.findViewById(R.id.recyclerViewSms)
        recyclerViewSms.layoutManager = LinearLayoutManager(requireContext())

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

        val tokenizerBert = Tokenizer(requireContext(), "vocab_kobert.txt", "tokenizer_config_kobert.json")
        val tokenizerElectra = Tokenizer(requireContext(), "vocab_koelectra.txt", "tokenizer_config_koelectra.json")

        val classifierBert = ModelClassifier(ortEnv, tokenizerBert, sessionKoBert)
        val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, sessionKoElectra)

        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                val (spam1, _) = classifierBert.classify(body)
                val (spam2, _) = classifierElectra.classify(body)

                val isSpam = spam1 >= 60.0f && spam2 >= 60.0f
                smsList.add(Pair(body, isSpam))
            }
        }

        recyclerViewSms.adapter = SmsAdapter(smsList)
    }
}