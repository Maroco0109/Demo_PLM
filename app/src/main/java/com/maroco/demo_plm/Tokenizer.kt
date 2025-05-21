package com.maroco.demo_plm

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class Tokenizer(
    context: Context,
    private val vocabFile: String,
    private val configFile: String,
    private val subwordPrefix: String = "##"    // 기본 WordPiece prefix
) {
    private val vocab: Map<String, Int>
    private val unkToken: String
    private val clsToken: String
    private val sepToken: String
    private val padToken: String
    private val maxLen: Int
    private val doLowerCase: Boolean

    init {
        // 1) vocab 로드
        val vocabMap = mutableMapOf<String, Int>()
        context.assets.open(vocabFile).bufferedReader().useLines { lines ->
            lines.forEachIndexed { idx, line -> vocabMap[line.trim()] = idx }
        }
        vocab = vocabMap

        // 2) tokenizer_config.json 로드
        val jsonStr = context.assets.open(configFile).bufferedReader().readText()
        val config = JSONObject(jsonStr)
        doLowerCase = config.optBoolean("do_lower_case", true)
        maxLen     = config.optInt("model_max_length", 64)

        // 3) special tokens: JSON에 있으면 우선, 없으면 기본값
        unkToken = config.optString("unk_token", "[UNK]")
        clsToken = config.optString("cls_token", "[CLS]")
        sepToken = config.optString("sep_token", "[SEP]")
        padToken = config.optString("pad_token", "[PAD]")
    }

    private fun wordpieceTokenize(word: String): List<String> {
        val tokens = mutableListOf<String>()
        var start = 0
        val len = word.length

        while (start < len) {
            var end = len
            var sub: String? = null

            while (start < end) {
                var piece = word.substring(start, end)
                if (start > 0) piece = subwordPrefix + piece
                if (vocab.containsKey(piece)) {
                    sub = piece
                    break
                }
                end--
            }
            if (sub != null) {
                tokens.add(sub)
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
        val words = cleaned.split("\\s+".toRegex())
        val wpTokens = words.flatMap { wordpieceTokenize(it) }

        val tokens = listOf(clsToken) +
                wpTokens.take(maxLen - 2) +
                listOf(sepToken)

        val inputIds     = tokens.map { vocab[it] ?: vocab[unkToken] ?: 0 }
        val attention    = List(inputIds.size) { 1 }
        val tokenTypeIds = List(inputIds.size) { 0 }

        // padding
        val padLen = maxLen - inputIds.size
        val paddedIds     = inputIds + List(padLen) { vocab[padToken] ?: 0 }
        val paddedAttn    = attention + List(padLen) { 0 }
        val paddedTypeIds = tokenTypeIds + List(padLen) { 0 }

        return Triple(
            paddedIds.toIntArray(),
            paddedAttn.toIntArray(),
            paddedTypeIds.toIntArray()
        )
    }
}
