package com.maroco.demo_plm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import ai.onnxruntime.OrtEnvironment

class InputFragment : Fragment() {

    private lateinit var editText: EditText
    private lateinit var classifyButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_input, container, false)

        editText = view.findViewById(R.id.inputText)
        classifyButton = view.findViewById(R.id.btnClassify)
        resultTextView = view.findViewById(R.id.resultView)

        classifyButton.setOnClickListener {
            classifyInputText()
        }

        return view
    }

    private fun classifyInputText() {
        val text = editText.text.toString()
        val ortEnv = OrtEnvironment.getEnvironment()

        val tokenizerBert = Tokenizer(requireContext(), "vocab_kobert.txt", "tokenizer_config_kobert.json")
        val tokenizerElectra = Tokenizer(requireContext(), "vocab_koelectra.txt", "tokenizer_config_koelectra.json")

        val sessionBert = (activity as MainActivity).sessionKoBert
        val sessionElectra = (activity as MainActivity).sessionKoElectra

        val classifierBert = ModelClassifier(ortEnv, tokenizerBert, sessionBert)
        val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, sessionElectra)

        val (spamBert, hamBert) = classifierBert.classify(text)
        val (spamElectra, hamElectra) = classifierElectra.classify(text)

        resultTextView.text = """
            [KoBERT]   스팸 %.1f%%, 햄 %.1f%%
            [ELECTRA] 스팸 %.1f%%, 햄 %.1f%%
        """.trimIndent().format(spamBert, hamBert, spamElectra, hamElectra)
    }
}