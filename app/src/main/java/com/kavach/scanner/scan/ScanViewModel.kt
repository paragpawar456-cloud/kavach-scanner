package com.kavach.scanner.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kavach.core.ScanEngine
import com.kavach.core.model.InputType
import com.kavach.core.model.ScanReport
import com.kavach.core.model.Verdict
import com.kavach.scanner.data.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Scanning(val label: String) : ScanUiState
    data class Done(val report: ScanReport, val extractedText: String? = null, val extra: List<ScanReport> = emptyList()) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)
    private val engine = ScanEngine()

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state

    /** Fires whenever a scan starts so the UI can open the result screen (also for scans shared from other apps). */
    private val _openResult = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openResult: SharedFlow<Unit> = _openResult

    private var job: Job? = null

    fun scan(input: String, type: InputType) {
        if (input.isBlank()) return
        start(if (type == InputType.QR) "Analysing QR code…" else "Running security checks…") {
            ScanUiState.Done(engine.scan(input, type, settings.toOptions()))
        }
    }

    fun scanImage(uri: Uri) {
        start("Reading screenshot…") {
            val content = ImageScanner.read(getApplication(), uri)
            if (content.text.isBlank() && content.qrCodes.isEmpty()) {
                return@start ScanUiState.Error("No text or QR code could be read from this image. Try a clearer screenshot, or paste the message text instead.")
            }
            _state.value = ScanUiState.Scanning("Checking ${content.qrCodes.size + if (content.text.isNotBlank()) 1 else 0} item(s)…")
            val opts = settings.toOptions()
            val text = if (content.text.isNotBlank()) engine.scan(content.text, InputType.SCREENSHOT, opts) else null
            val qrs = content.qrCodes.take(3).map { viewModelScope.async { engine.scan(it, InputType.QR, opts) } }.awaitAll()
            val all = listOfNotNull(text) + qrs
            // Show the most dangerous result first; the others are listed underneath.
            val sorted = all.sortedByDescending { it.riskScore + if (it.verdict == Verdict.DANGEROUS) 1000 else 0 }
            ScanUiState.Done(sorted.first(), extractedText = content.text.ifBlank { null }, extra = sorted.drop(1))
        }
    }

    fun reset() {
        job?.cancel()
        _state.value = ScanUiState.Idle
    }

    private fun start(label: String, block: suspend () -> ScanUiState) {
        job?.cancel()
        _state.value = ScanUiState.Scanning(label)
        _openResult.tryEmit(Unit)
        job = viewModelScope.launch {
            _state.value = try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ScanUiState.Error("Scan failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }
}
