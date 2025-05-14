package com.maroco.demo_plm

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class Tokenizer(context: Context, vocabFile: String, configFile: String) {

    private val vocab: Map<String, Int>
    private val unkToken = "[UNK]"
    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val padToken = "[PAD]"
    private val maxLen = 64
    private val doLowerCase: Boolean

    init {
        // vocab.txt 로드
        val vocabMap = mutableMapOf<String, Int>()
        context.assets.open(vocabFile).bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                vocabMap[line.trim()] = index
            }
        }
        vocab = vocabMap

        // tokenizer_config.json 로드
        val jsonStr = context.assets.open(configFile).bufferedReader().use { it.readText() }
        val config = JSONObject(jsonStr)
        doLowerCase = config.optBoolean("do_lower_case", true)
    }

    private fun wordpieceTokenize(word: String): List<String> {
        val tokens = mutableListOf<String>()
        var start = 0
        val length = word.length

        while (start < length) {
            var end = length
            var subword: String? = null

            while (start < end) {
                var candidate = word.substring(start, end)
                if (start > 0) candidate = "##$candidate"
                if (vocab.containsKey(candidate)) {
                    subword = candidate
                    break
                }
                end--
            }

            if (subword != null) {
                tokens.add(subword)
                start = end
            } else {
                tokens.add(unkToken)
                break
            }
        }

        return tokens
    }

    fun tokenize(text: String): Triple<IntArray, IntArray, IntArray> {
        val cleaned = if (doLowerCase) text.lowercase() else text
        val words = cleaned.split(" ", "\n", "\t", "\r")
        val wordpieceTokens = words.flatMap { wordpieceTokenize(it) }

        val tokens = listOf(clsToken) + wordpieceTokens.take(maxLen - 2) + listOf(sepToken)
        val inputIds = tokens.map { vocab[it] ?: vocab[unkToken] ?: 0 }
        val attentionMask = List(inputIds.size) { 1 }
        val tokenTypeIds = List(inputIds.size) { 0 }

        // 패딩
        val padLength = maxLen - inputIds.size
        val paddedInputIds = inputIds + List(padLength) { vocab[padToken] ?: 0 }
        val paddedAttentionMask = attentionMask + List(padLength) { 0 }
        val paddedTokenTypeIds = tokenTypeIds + List(padLength) { 0 }

        return Triple(
            paddedInputIds.toIntArray(),
            paddedAttentionMask.toIntArray(),
            paddedTokenTypeIds.toIntArray()
        )
    }
}