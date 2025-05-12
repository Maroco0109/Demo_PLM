package com.maroco.demo_plm

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ai.onnxruntime.*
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var sessionBert: OrtSession
    private lateinit var sessionElectra: OrtSession
    private lateinit var sessionRoberta: OrtSession

    private lateinit var vocabBert: Map<String, Int>
    private lateinit var vocabElectra: Map<String, Int>
    private lateinit var vocabRoberta: Map<String, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ortEnv = OrtEnvironment.getEnvironment()

        sessionBert = ortEnv.createSession(loadModelFile("kobert_spam.onnx"))
        sessionElectra = ortEnv.createSession(loadModelFile("koelectra_spam.onnx"))
        sessionRoberta = ortEnv.createSession(loadModelFile("koroberta_spam.onnx"))

        vocabBert = loadVocab("vocab_kobert.txt")
        vocabElectra = loadVocab("vocab_koelectra.txt")
        vocabRoberta = loadVocab("vocab_koroberta.txt")

        val inputText = findViewById<EditText>(R.id.inputText)
        val btnClassify = findViewById<Button>(R.id.btnClassify)
        val resultView = findViewById<TextView>(R.id.resultView)

        btnClassify.setOnClickListener {
            val input = inputText.text.toString()
            val result = classifyWithAllModels(input)
            resultView.text = result
        }
    }

    private fun loadModelFile(name: String): String {
        val file = File(cacheDir, name)
        if (!file.exists()) {
            assets.open(name).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    private fun loadVocab(fileName: String): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        assets.open(fileName).bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEachIndexed { idx, token -> vocab[token.trim()] = idx }
        }
        return vocab
    }

    private fun classifyWithAllModels(text: String): String {
        val results = StringBuilder("결과:\n")

        val modelSet = listOf(
            Triple("BERT", sessionBert, vocabBert),
            Triple("ELECTRA", sessionElectra, vocabElectra),
            Triple("RoBERTa", sessionRoberta, vocabRoberta)
        )

        for ((name, session, vocab) in modelSet) {
            val (inputIds, attentionMask) = tokenizeText(text, vocab)
            val inputTensor = OnnxTensor.createTensor(ortEnv, arrayOf(inputIds))
            val attentionTensor = OnnxTensor.createTensor(ortEnv, arrayOf(attentionMask))

            val outputs = session.run(mapOf(
                "input_ids" to inputTensor,
                "attention_mask" to attentionTensor
            ))

            val logits = (outputs[0].value as Array<FloatArray>)[0]
            val probs = softmax(logits)
            val spam = probs[0] * 100
            val ham = probs[1] * 100

            results.append("- $name: 스팸 %.1f%%, 햄 %.1f%%\n".format(spam, ham))
        }

        return results.toString().trim()
    }

    private fun tokenizeText(text: String, vocab: Map<String, Int>): Pair<LongArray, LongArray> {
        val tokens = text.trim().split(" ").filter { it.isNotBlank() }
        val tokenIds = tokens.map { vocab[it] ?: vocab["[UNK]"] ?: 0 }
        val maxLen = 128
        val padded = tokenIds.take(maxLen) + List(maxLen - tokenIds.size) { 0 }
        val attention = List(padded.size) { if (padded[it] != 0) 1L else 0L }

        return Pair(padded.map { it.toLong() }.toLongArray(), attention.toLongArray())
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { Math.exp((it - max).toDouble()) }
        val sum = exps.sum()
        return exps.map { (it / sum).toFloat() }.toFloatArray()
    }
}