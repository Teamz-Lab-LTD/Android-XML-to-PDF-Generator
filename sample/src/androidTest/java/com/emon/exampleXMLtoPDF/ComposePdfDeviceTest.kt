package com.emon.exampleXMLtoPDF

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class ComposePdfDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComposeDemoActivity>()
    @Test fun renderedComposeCardExportsNonBlankPdf() {
        val file = File(compose.activity.getExternalFilesDir(null), "compose-receipt.pdf")
        file.delete()
        compose.onNodeWithTag("export_compose").performClick()
        compose.waitUntil(20000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("PDF ready · 1 page(s)")).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(file.length() > 0)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { pdf ->
                assertEquals(1, pdf.pageCount)
                pdf.openPage(0).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                    val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    var ink = 0
                    for (y in 0 until 842 step 4) for (x in 0 until 595 step 4) {
                        val pixel = bitmap.getPixel(x, y)
                        if (Color.red(pixel) < 180 && Color.green(pixel) < 180) ink++
                    }
                    assertTrue("The exported Compose card must contain visible content", ink > 80)
                    bitmap.recycle()
                }
            }
        }
    }
}
