package com.emon.exampleXMLtoPDF

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gkemon.pdf.compose.PdfCapture
import com.gkemon.pdf.compose.rememberPdfCaptureState
import com.gkemon.XMLtoPDF.PdfGenerator
import com.gkemon.XMLtoPDF.PdfGeneratorListener
import com.gkemon.XMLtoPDF.model.FailureResponse
import com.gkemon.XMLtoPDF.model.SuccessResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class ComposeDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val green = Color(0xFF173D32)
            MaterialTheme(colorScheme = lightColorScheme(primary = green, background = Color(0xFFF4F2EC))) {
                val capture = rememberPdfCaptureState()
                val scope = rememberCoroutineScope()
                var status by remember { mutableStateOf("Ready to export the card below.") }
                var busy by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Text("TEAMZ LAB · COMPOSE", color = green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    Text("From composable\nto shareable.", color = green, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("A real Compose capture. Export the rendered card as an A4 PDF.", color = Color(0xFF56645D))
                    Spacer(Modifier.height(24.dp))
                    PdfCapture(capture, Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().background(Color.White).padding(24.dp)) {
                            Text("PROJECT RECEIPT", color = Color(0xFF27745A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(20.dp))
                            Text("Made with\nJetpack Compose", color = green, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0xFFDBE2DC))
                            Spacer(Modifier.height(20.dp))
                            Text("Design system                       Complete\nPDF export                             Ready\nStorage permissions              None", color = green, lineHeight = 30.sp, fontSize = 14.sp)
                            Spacer(Modifier.height(24.dp))
                            Text("SAMPLE / 001", color = Color(0xFF56645D), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("export_compose"), enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    capture.export(this@ComposeDemoActivity, "compose-receipt",
                                        listener = object : PdfGeneratorListener() {
                                            override fun onSuccess(response: SuccessResponse) { status = "PDF ready · ${response.pageCount} page(s)" }
                                            override fun onFailure(response: FailureResponse) { status = "Export failed: ${response.errorMessage}" }
                                            override fun onFinishPDFGeneration() { busy = false }
                                        })
                                } catch (cancelled: CancellationException) { busy = false; throw cancelled }
                                catch (e: Exception) { status = e.message ?: "Export failed"; busy = false }
                            }
                        }) { Text(if (busy) "Exporting…" else "Export Compose PDF") }
                    Text(status, modifier = Modifier.padding(top = 12.dp).testTag("compose_status"), color = green, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Captures the drawn frame as an image. Off-screen lazy-list items are not included.", color = Color(0xFF56645D), fontSize = 12.sp)
                    TextButton(onClick = { finish() }) { Text("← XML examples") }
                }
            }
        }
    }
}
