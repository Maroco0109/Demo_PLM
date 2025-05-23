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
import java.io.File
import java.io.FileWriter

class InputFragment : Fragment() {
    private var logWriter: FileWriter? = null
    private lateinit var editText: EditText
    private lateinit var classifyButton: Button
    private lateinit var resultTextView: TextView
    private val ortEnv by lazy { OrtEnvironment.getEnvironment() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ONNX 세션 초기화 (ModelManager 사용)
        val sessionBert    = ModelManager.sessionKoBert
        val sessionElectra = ModelManager.sessionKoElectra
        val sessionRoberta = ModelManager.sessionKoRoberta

        // Tokenizer 인스턴스
        val tokenizerBert    = Tokenizer(requireContext(),
            "vocab_kobert.txt",    "tokenizer_config_kobert.json",    subwordPrefix = "##")
        val tokenizerElectra = Tokenizer(requireContext(),
            "vocab_koelectra.txt", "tokenizer_config_koelectra.json", subwordPrefix = "##")
        val tokenizerRoberta = Tokenizer(requireContext(),
            "vocab_koroberta.txt", "tokenizer_config_koroberta.json", subwordPrefix = "Ġ")

        // 분류기 인스턴스
        val classifierBert    = ModelClassifier(ortEnv, tokenizerBert,    sessionBert)
        val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, sessionElectra)
        val classifierRoberta = ModelClassifier(ortEnv, tokenizerRoberta, sessionRoberta)

        // 로그 디렉터리 및 파일 초기화
        val logDir = File(requireContext().filesDir, "main/logs")
        if (!logDir.exists()) logDir.mkdirs()
        logWriter = FileWriter(File(logDir, "input_log.txt"), /* append= */ true).apply {
            write("=== New Session InputFragment: ${System.currentTimeMillis()} ===\n\n")
            flush()
        }

        val view = inflater.inflate(R.layout.fragment_input, container, false)
        editText       = view.findViewById(R.id.inputText)
        classifyButton = view.findViewById(R.id.btnClassify)
        resultTextView = view.findViewById(R.id.resultView)

        classifyButton.setOnClickListener {
            val text = editText.text.toString().trim()
            if (text.isEmpty()) {
                resultTextView.text = "입력된 텍스트가 없습니다."
                return@setOnClickListener
            }

            // 토큰화(ID) 로그
            val (idsB, _, _) = tokenizerBert.tokenize(text)
            val (idsE, _, _) = tokenizerElectra.tokenize(text)
            val (idsR, _, _) = tokenizerRoberta.tokenize(text)
            logWriter?.apply {
                write("[Input] $text\n")
                write("BERT IDs:    ${idsB.joinToString(" ")}\n")
                write("ELECTRA IDs: ${idsE.joinToString(" ")}\n")
                write("ROBERTA IDs: ${idsR.joinToString(" ")}\n")
            }

            // 모델별 분류 결과
            val (spamB, hamB) = classifierBert.classify(text)
            val (spamE, hamE) = classifierElectra.classify(text)
            val (spamR, hamR) = classifierRoberta.classify(text)

            // Soft Voting 앙상블
            val ensembleProb = 0.2f * spamB + 0.4f * spamE + 0.4f * spamR
            val isSpam = ensembleProb >= 50f

            // 개별 및 앙상블 결과 로그
            logWriter?.apply {
                write("[BERT]    Spam: ${"%.2f".format(spamB)}%, Ham: ${"%.2f".format(hamB)}%\n")
                write("[ELECTRA] Spam: ${"%.2f".format(spamE)}%, Ham: ${"%.2f".format(hamE)}%\n")
                write("[ROBERTA] Spam: ${"%.2f".format(spamR)}%, Ham: ${"%.2f".format(hamR)}%\n")
                write("[ENSEMBLE] Spam: ${"%.2f".format(ensembleProb)}% → ${if (isSpam) "SPAM" else "HAM"}\n\n")
                flush()
            }

            // UI에 출력
            val output = """
                [BERT]    Spam: ${"%.2f".format(spamB)}%, Ham: ${"%.2f".format(hamB)}%
                [ELECTRA] Spam: ${"%.2f".format(spamE)}%, Ham: ${"%.2f".format(hamE)}%
                [ROBERTA] Spam: ${"%.2f".format(spamR)}%, Ham: ${"%.2f".format(hamR)}%
                [ENSEMBLE] Spam: ${"%.2f".format(ensembleProb)}% → ${if (isSpam) "SPAM" else "HAM"}
            """.trimIndent()
            resultTextView.text = output
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logWriter?.close()
    }
}
