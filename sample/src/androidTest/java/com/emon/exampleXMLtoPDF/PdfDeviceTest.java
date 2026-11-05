package com.emon.exampleXMLtoPDF;

import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.gkemon.XMLtoPDF.PdfGenerator;
import com.gkemon.XMLtoPDF.PdfGeneratorListener;
import com.gkemon.XMLtoPDF.model.FailureResponse;
import com.gkemon.XMLtoPDF.model.SuccessResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PdfDeviceTest {
    static class Result extends PdfGeneratorListener {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicInteger finishes = new AtomicInteger();
        SuccessResponse success; FailureResponse failure;
        @Override public void onSuccess(SuccessResponse result) { success = result; }
        @Override public void onFailure(FailureResponse error) { failure = error; }
        @Override public void onFinishPDFGeneration() { finishes.incrementAndGet(); done.countDown(); }
        void await() throws Exception { assertTrue("callback timed out", done.await(20, TimeUnit.SECONDS)); assertEquals(1, finishes.get()); }
    }
    private TextView content(MainActivity a, int width, int height) {
        TextView view = new TextView(a);
        view.setText("PDF regression test / বাংলা"); view.setTextColor(Color.BLACK);
        view.setBackgroundColor(Color.WHITE);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return view;
    }
    @Test public void appPrivateA4NeedsNoStoragePermission() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Result result = new Result();
            scenario.onActivity(a -> PdfGenerator.getBuilder().setContext(a).fromLayoutXMLSource()
                    .fromLayoutXML(R.layout.document_invoice).setPageSize(PdfGenerator.PageSize.A4)
                    .setFileName("device-invoice.pdf").setPageMargin(24)
                    .actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE).build(result));
            result.await(); assertNull(result.failure); assertNotNull(result.success.getUri());
            assertEquals("device-invoice.pdf", result.success.getFile().getName());
            assertPdf(result.success, 1, 595, 842);
        }
    }
    @Test public void tallViewSplitsAcrossA4Pages() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Result result = new Result();
            scenario.onActivity(a -> PdfGenerator.getBuilder().setContext(a).fromViewSource()
                    .fromView(content(a, 595, 2000)).setPageSize(PdfGenerator.PageSize.A4)
                    .setFileName("device-long").actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE).build(result));
            result.await(); assertNull(result.failure); assertPdf(result.success, 3, 595, 842);
        }
    }
    @Test public void a5LandscapeHasRealPaperDimensions() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Result result = new Result();
            scenario.onActivity(a -> PdfGenerator.getBuilder().setContext(a).fromViewSource()
                    .fromView(content(a, 595, 300)).setPageSize(PdfGenerator.PageSize.A5)
                    .setFileName("device-landscape").setLandscape(true)
                    .actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE).build(result));
            result.await(); assertNull(result.failure); assertPdf(result.success, 1, 595, 420);
        }
    }
    @Test public void multipleXmlLayoutsBecomeMultiplePages() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Result result = new Result();
            scenario.onActivity(a -> PdfGenerator.getBuilder().setContext(a).fromLayoutXMLSource()
                    .fromLayoutXML(R.layout.document_invoice, R.layout.document_invoice)
                    .setFileName("device-multiple").actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE).build(result));
            result.await(); assertNull(result.failure); assertPdf(result.success, 2, 595, 760);
        }
    }
    @Test public void attachedViewBoundsAreRestored() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Result result = new Result();
            scenario.onActivity(a -> {
                View view = a.findViewById(R.id.export_invoice);
                int left = view.getLeft(), top = view.getTop(), width = view.getWidth(), height = view.getHeight();
                PdfGenerator.getBuilder().setContext(a).fromViewIDSource().fromViewID(a, R.id.export_invoice)
                        .setFileName("device-view-id").actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE).build(result);
                assertEquals(left, view.getLeft()); assertEquals(top, view.getTop());
                assertEquals(width, view.getWidth()); assertEquals(height, view.getHeight());
            });
            result.await(); assertNull(result.failure);
        }
    }
    @Test public void emptySourceFailsOnce() throws Exception { reject(0); }
    @Test public void missingViewFailsOnce() throws Exception { reject(1); }
    @Test public void nullViewFailsOnce() throws Exception { reject(2); }
    @Test public void invalidMarginsFailOnce() throws Exception { reject(3); }
    @Test public void invalidNameFailsOnce() throws Exception { reject(4); }
    private void reject(int mode) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Result result = new Result();
            scenario.onActivity(a -> {
                PdfGenerator.FromSourceStep start = PdfGenerator.getBuilder().setContext(a);
                PdfGenerator.FileNameStep source = mode == 0 ? start.fromViewSource().fromView()
                        : mode == 1 ? start.fromViewIDSource().fromViewID(a, -1)
                        : mode == 2 ? start.fromViewSource().fromView((View) null)
                        : start.fromViewSource().fromView(content(a, 595, 500));
                source.setPageSize(PdfGenerator.PageSize.A4).setFileName(mode == 4 ? "../escape" : "device-invalid")
                        .setPageMargin(mode == 3 ? 400 : 0)
                        .actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE).build(result);
            });
            result.await(); assertNull(result.success); assertNotNull(result.failure);
        }
    }
    @Test public void sharedPickerCancelAndRotationDoNotCrash() throws Exception {
        androidx.test.uiautomator.UiDevice device = androidx.test.uiautomator.UiDevice.getInstance(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation());
        AtomicInteger creations = new AtomicInteger();
        androidx.test.runner.lifecycle.ActivityLifecycleCallback callback = (activity, stage) -> {
            if (activity instanceof MainActivity && stage == androidx.test.runner.lifecycle.Stage.CREATED) creations.incrementAndGet();
        };
        androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.generate(true));
            assertTrue("Save picker did not open", device.wait(androidx.test.uiautomator.Until.hasObject(
                    androidx.test.uiautomator.By.pkg(java.util.regex.Pattern.compile("com(\\.google)?\\.android\\.documentsui"))), 10000));
            device.setOrientationLeft();
            device.waitForIdle();
            device.pressBack();
            // A rotated document picker may focus the filename and show the IME.
            // The first Back dismisses that keyboard; a second exits the picker.
            if (device.wait(androidx.test.uiautomator.Until.hasObject(
                    androidx.test.uiautomator.By.pkg("com.emon.XMLtoPDF")), 1500) == false) {
                device.pressBack();
            }
            assertTrue("Host did not return from the picker", device.wait(androidx.test.uiautomator.Until.hasObject(
                    androidx.test.uiautomator.By.pkg("com.emon.XMLtoPDF")), 10000));
            // The status is below the fold in landscape. Assert its bound value,
            // rather than requiring UIAutomator to find off-screen text.
            scenario.onActivity(a -> assertEquals("Save cancelled. You can try again.",
                    ((TextView) a.findViewById(R.id.status)).getText().toString()));
            assertTrue("The host Activity must actually recreate", creations.get() >= 2);
        } finally {
            device.setOrientationNatural(); device.unfreezeRotation();
            androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback);
        }
    }
    @Test public void sharedPickerWritesReadablePdf() throws Exception {
        androidx.test.uiautomator.UiDevice device = androidx.test.uiautomator.UiDevice.getInstance(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation());
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.generate(true));
            androidx.test.uiautomator.UiObject2 name = device.wait(androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.clazz("android.widget.EditText")), 10000);
            assertNotNull("Filename field not found", name);
            name.setText("layout-to-pdf-test-" + System.currentTimeMillis() + ".pdf");
            androidx.test.uiautomator.UiObject2 save = device.wait(androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.text(java.util.regex.Pattern.compile("(?i)save"))), 10000);
            assertNotNull("Save button not found", save);
            save.click();
            assertTrue("Host did not return from save", device.wait(androidx.test.uiautomator.Until.hasObject(
                    androidx.test.uiautomator.By.pkg("com.emon.XMLtoPDF")), 10000));
            java.util.concurrent.atomic.AtomicBoolean previewed = new java.util.concurrent.atomic.AtomicBoolean();
            for (int i = 0; i < 40 && !previewed.get(); i++) {
                scenario.onActivity(a -> previewed.set("PAGE 1 / 1".equals(
                        ((TextView) a.findViewById(R.id.page_label)).getText().toString())));
                if (!previewed.get()) Thread.sleep(250);
            }
            assertTrue("Saved URI must open successfully with PdfRenderer", previewed.get());
            scenario.onActivity(a -> assertEquals("PDF ready · 1 page(s)",
                    ((TextView) a.findViewById(R.id.status)).getText().toString()));
        }
    }
    private void assertPdf(SuccessResponse result, int count, int width, int height) throws Exception {
        assertNotNull(result); assertEquals(count, result.getPageCount());
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(result.getFile(), ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(fd)) {
            assertEquals(count, renderer.getPageCount());
            for (int i = 0; i < count; i++) try (PdfRenderer.Page page = renderer.openPage(i)) {
                assertEquals(width, page.getWidth()); assertEquals(height, page.getHeight());
            }
        }
    }
}
