package com.kavach.scanner.data

import com.kavach.core.model.InputType
import com.kavach.core.model.ScanReport
import com.kavach.core.model.Verdict
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(val time: Long, val input: String, val type: InputType, val verdict: Verdict, val score: Int, val topReason: String?)

/** Keeps the last 50 scans on this phone only (encrypted preferences). Users can turn it off or clear it. */
class HistoryStore(private val settings: SettingsStore) {

    fun all(): List<HistoryItem> = try {
        val arr = JSONArray(settings.historyJson)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryItem(
                o.getLong("t"), o.getString("i"),
                runCatching { InputType.valueOf(o.getString("ty")) }.getOrDefault(InputType.URL),
                runCatching { Verdict.valueOf(o.getString("v")) }.getOrDefault(Verdict.SUSPICIOUS),
                o.getInt("s"), o.optString("r").ifBlank { null },
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun add(r: ScanReport) {
        if (!settings.saveHistory) return
        val item = HistoryItem(System.currentTimeMillis(), r.input.take(300), r.inputType, r.verdict, r.riskScore, r.findings.firstOrNull { it.risk > 0 }?.title)
        val list = (listOf(item) + all()).take(50)
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("t", it.time).put("i", it.input).put("ty", it.type.name).put("v", it.verdict.name).put("s", it.score).put("r", it.topReason ?: ""))
        }
        settings.historyJson = arr.toString()
    }

    fun clear() { settings.historyJson = "[]" }

    fun stats(): Triple<Int, Int, Int> {
        val a = all()
        return Triple(a.size, a.count { it.verdict == Verdict.DANGEROUS }, a.count { it.verdict == Verdict.SUSPICIOUS })
    }
}
