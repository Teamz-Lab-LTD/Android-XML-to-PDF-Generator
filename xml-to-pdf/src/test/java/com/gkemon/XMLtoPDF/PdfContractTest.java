package com.gkemon.XMLtoPDF;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PdfContractTest {
    @Test public void filenameKeepsSpacesAndAvoidsDuplicateExtension() {
        assertEquals("Invoice 01.pdf", PdfGenerator.safeFileName(" Invoice 01.pdf "));
        assertEquals("Invoice.PDF", PdfGenerator.safeFileName("Invoice.PDF"));
        assertEquals("invoice.pdf", PdfGenerator.safeFileName("invoice"));
    }
    @Test public void filenamesRejectPathsAndEmptyInput() {
        for (String name : new String[]{null, "", "  ", "..", "../secret", "folder/file", "a\\b", "x\u0000"}) {
            assertThrows(IllegalArgumentException.class, () -> PdfGenerator.safeFileName(name));
        }
    }
    @Test public void foldersStayInsideAppStorage() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File file = PdfGenerator.outputFile(context, "Invoices/November", "invoice.pdf");
        assertTrue(file.getCanonicalPath().startsWith(context.getExternalFilesDir(null).getCanonicalPath() + "/"));
        assertThrows(IOException.class, () -> PdfGenerator.outputFile(context, "../../escaped", "invoice.pdf"));
        assertThrows(IOException.class, () -> PdfGenerator.outputFile(context, "/storage/emulated/0/Download", "invoice.pdf"));
    }
    @Test public void internalAbsoluteFolderIsSupported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File folder = new File(context.getFilesDir(), "PDFs");
        assertEquals(new File(folder, "invoice.pdf"), PdfGenerator.outputFile(context, folder.getPath(), "invoice.pdf"));
    }
    @Test public void sharingGrantsReadOnlyAndIncludesClipData() {
        Uri uri = Uri.parse("content://test/doc/1");
        Intent intent = PdfGenerator.createPdfIntent(uri, PdfGenerator.ActionAfterPDFGeneration.SHARE);
        assertEquals(Intent.ACTION_SEND, intent.getAction());
        assertEquals("application/pdf", intent.getType());
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM));
        assertEquals(uri, intent.getClipData().getItemAt(0).getUri());
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.getFlags());
    }
}
