package com.gkemon.pdf.compose

import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.gkemon.XMLtoPDF.PdfGenerator
import com.gkemon.XMLtoPDF.PdfGeneratorListener
import com.gkemon.XMLtoPDF.model.FailureResponse
import com.gkemon.XMLtoPDF.model.SuccessResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Captures the last drawn frame, not uncomposed or off-screen lazy-list items. */
@Stable
class PdfCaptureState internal constructor(private val layer: GraphicsLayer) {
    internal var attached = false
    internal var drawn = false

    internal fun recordLayer(scope: androidx.compose.ui.graphics.drawscope.ContentDrawScope, content: () -> Unit) {
        with(scope) {
            layer.record { content() }
            drawLayer(layer)
        }
        drawn = true
    }

    /** Caller owns the returned software bitmap. Wait until content (including images) is drawn. */
    suspend fun captureBitmap(): Bitmap = withContext(Dispatchers.Main.immediate) {
        check(attached && drawn) { "PdfCapture must be attached and drawn before exporting." }
        require(layer.size.width.toLong() * layer.size.height <= 16_000_000L) {
            "Capture exceeds 16 megapixels. Export smaller document sections."
        }
        val image = layer.toImageBitmap().asAndroidBitmap()
        try {
            checkNotNull(image.copy(Bitmap.Config.ARGB_8888, false)) { "Unable to copy the captured frame." }
        } finally { image.recycle() }
    }

    /** Raster PDF from the rendered frame. Writing is asynchronous; completion uses listener. */
    suspend fun export(
        activity: ComponentActivity,
        fileName: String,
        pageSize: PdfGenerator.PageSize = PdfGenerator.PageSize.A4,
        landscape: Boolean = false,
        marginPoints: Int = 24,
        action: PdfGenerator.ActionAfterPDFGeneration = PdfGenerator.ActionAfterPDFGeneration.NONE,
        saveObserver: PdfGenerator.XmlToPDFLifecycleObserver? = null,
        listener: PdfGeneratorListener = object : PdfGeneratorListener() {}
    ) = withContext(Dispatchers.Main.immediate) {
        val bitmap = captureBitmap()
        val view = ImageView(activity).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = ViewGroup.LayoutParams(bitmap.width, bitmap.height)
        }
        val builder = PdfGenerator.getBuilder().setContext(activity)
            .fromViewSource().fromView(view).setPageSize(pageSize).setFileName(fileName)
            .setLandscape(landscape).setPageMargin(marginPoints).actionAfterPDFGeneration(action)
        if (saveObserver != null) builder.savePDFSharedStorage(saveObserver)
        builder.build(object : PdfGeneratorListener() {
            override fun onStartPDFGeneration() = listener.onStartPDFGeneration()
            override fun showLog(log: String) = listener.showLog(log)
            override fun onSuccess(response: SuccessResponse) = listener.onSuccess(response)
            override fun onFailure(response: FailureResponse) = listener.onFailure(response)
            override fun onFinishPDFGeneration() {
                bitmap.recycle()
                listener.onFinishPDFGeneration()
            }
        })
    }
}

@Composable
fun rememberPdfCaptureState(): PdfCaptureState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { PdfCaptureState(layer) }
}

/** Use one capture container per state. Give document content its own light print theme. */
@Composable
fun PdfCapture(state: PdfCaptureState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    DisposableEffect(state) {
        check(!state.attached) { "Use a separate PdfCaptureState for each PdfCapture." }
        state.attached = true
        onDispose { state.attached = false; state.drawn = false }
    }
    Box(modifier.drawWithContent {
        state.record(this) { this@drawWithContent.drawContent() }
    }) { content() }
}

private fun PdfCaptureState.record(scope: androidx.compose.ui.graphics.drawscope.ContentDrawScope, content: () -> Unit) {
    // Kept internal to the container so callers cannot record another frame during capture.
    recordLayer(scope, content)
}
