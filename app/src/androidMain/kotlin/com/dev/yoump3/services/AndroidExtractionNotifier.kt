package com.dev.yoump3.services

import android.content.Context

/**
 * Implementación Android de [ExtractionNotifier]. No guarda estado propio: todo el ciclo de vida
 * del aviso vive en [ExtractionService], para que el ViewModel no dependa de Android.
 */
class AndroidExtractionNotifier(
    private val context: Context
) : ExtractionNotifier {

    override fun start(title: String, onCancel: () -> Unit) {
        ExtractionService.start(context, title, onCancel)
    }

    override fun finish(title: String, success: Boolean) {
        ExtractionService.finish(context, title, success)
    }

    override fun stop() {
        ExtractionService.stop(context)
    }
}
