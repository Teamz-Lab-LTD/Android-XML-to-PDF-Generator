package com.emon.exampleXMLtoPDF;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.gkemon.XMLtoPDF.PdfGenerator;
import com.gkemon.XMLtoPDF.PdfGeneratorListener;
import com.gkemon.XMLtoPDF.model.FailureResponse;
import com.gkemon.XMLtoPDF.model.SuccessResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, runnable example of XML, dynamic Views, scoped storage and PDF preview. */
public class MainActivity extends AppCompatActivity {
    private PdfGenerator.XmlToPDFLifecycleObserver saveObserver;
    private TextView status, pageLabel;
    private ImageView preview;
    private Uri lastUri;
    private int pageIndex, pageCount;
    private boolean report;
    private final ExecutorService previews = Executors.newSingleThreadExecutor();
    private Bitmap previewBitmap;
    private int previewRequest;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.screen_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        status = findViewById(R.id.status);
        pageLabel = findViewById(R.id.page_label);
        preview = findViewById(R.id.pdf_preview);
        saveObserver = new PdfGenerator.XmlToPDFLifecycleObserver(this);
        saveObserver.setResultListener(resultListener());
        getLifecycle().addObserver(saveObserver);
        findViewById(R.id.export_invoice).setOnClickListener(v -> { report = false; generate(false); });
        findViewById(R.id.export_report).setOnClickListener(v -> { report = true; generate(false); });
        findViewById(R.id.open_compose).setOnClickListener(v -> startActivity(new Intent(this, ComposeDemoActivity.class)));
        findViewById(R.id.save_pdf).setOnClickListener(v -> generate(true));
        findViewById(R.id.share_pdf).setOnClickListener(v -> share());
        findViewById(R.id.next_page).setOnClickListener(v -> { if (lastUri != null) { pageIndex = (pageIndex + 1) % pageCount; showPreview(); } });
        if (savedInstanceState != null) {
            report = savedInstanceState.getBoolean("report");
            String saved = savedInstanceState.getString("uri");
            if (saved != null) {
                lastUri = Uri.parse(saved); pageIndex = savedInstanceState.getInt("page");
                showPreview();
            }
        }
    }

    public void generate(boolean shared) {
        PdfGenerator.FileNameStep source = report
                ? PdfGenerator.getBuilder().setContext(this).fromViewSource().fromView(createReport())
                : PdfGenerator.getBuilder().setContext(this).fromLayoutXMLSource().fromLayoutXML(R.layout.document_invoice);
        PdfGenerator.Build builder = source.setPageSize(PdfGenerator.PageSize.A4)
                .setFileName(report ? "teamz-report" : "teamz-invoice")
                .setPageMargin(24).actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.NONE);
        if (shared) builder.savePDFSharedStorage(saveObserver);
        builder.build(resultListener());
    }

    private PdfGeneratorListener resultListener() {
        return new PdfGeneratorListener() {
            @Override public void onStartPDFGeneration() { status.setText(R.string.generating); setBusy(true); }
            @Override public void onSuccess(SuccessResponse response) {
                lastUri = response.getUri(); pageIndex = 0; pageCount = response.getPageCount();
                status.setText(getString(R.string.export_success, pageCount));
                showPreview();
            }
            @Override public void onFailure(FailureResponse response) {
                status.setText(response.getThrowable() instanceof java.util.concurrent.CancellationException
                        ? getString(R.string.save_cancelled) : getString(R.string.export_failed, response.getErrorMessage()));
            }
            @Override public void onFinishPDFGeneration() { setBusy(false); }
        };
    }
    private void setBusy(boolean busy) {
        for (int id : new int[]{R.id.export_invoice, R.id.export_report, R.id.save_pdf}) findViewById(id).setEnabled(!busy);
    }
    public View createReport() {
        LinearLayout document = new LinearLayout(this);
        document.setOrientation(LinearLayout.VERTICAL);
        document.setLayoutParams(new LinearLayout.LayoutParams(595, LinearLayout.LayoutParams.WRAP_CONTENT));
        document.setPadding(24, 20, 24, 20);
        document.setBackgroundColor(Color.WHITE);
        addLine(document, "TEAMZ LAB / PROJECT REPORT", 24, true);
        addLine(document, "Dynamic Views • A4 pagination", 15, false);
        for (int i = 1; i <= 60; i++) addLine(document, String.format(java.util.Locale.US, "%02d   Document row / Verified sample content", i), 17, false);
        addLine(document, "END OF REPORT", 18, true);
        return document;
    }
    private void addLine(LinearLayout parent, String text, int size, boolean heading) {
        TextView row = new TextView(this);
        row.setText(text); row.setTextColor(Color.rgb(23, 61, 50));
        row.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        row.setPadding(0, 9, 0, 9);
        if (heading) row.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(row);
    }
    private void showPreview() {
        final Uri uri = lastUri;
        final int index = pageIndex, request = ++previewRequest;
        previews.execute(() -> {
            try (ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r");
                 PdfRenderer renderer = new PdfRenderer(fd)) {
                int count = renderer.getPageCount();
                try (PdfRenderer.Page page = renderer.openPage(Math.min(index, count - 1))) {
                    Bitmap bitmap = Bitmap.createBitmap(714, Math.round(714f * page.getHeight() / page.getWidth()), Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    runOnUiThread(() -> {
                        if (isDestroyed() || request != previewRequest) { bitmap.recycle(); return; }
                        preview.setImageBitmap(bitmap);
                        if (previewBitmap != null) previewBitmap.recycle();
                        previewBitmap = bitmap; pageCount = count;
                        pageLabel.setText(getString(R.string.page_number, index + 1, count));
                        findViewById(R.id.preview_card).setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) { runOnUiThread(() -> { if (!isDestroyed()) status.setText(getString(R.string.preview_failed)); }); }
        });
    }
    private void share() {
        if (lastUri == null) { status.setText(R.string.generate_first); return; }
        Intent intent = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, lastUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("PDF", lastUri));
        startActivity(Intent.createChooser(intent, getString(R.string.share_pdf)));
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (lastUri != null) state.putString("uri", lastUri.toString());
        state.putInt("page", pageIndex); state.putBoolean("report", report);
    }
    @Override protected void onDestroy() {
        previews.shutdown();
        preview.setImageDrawable(null);
        if (previewBitmap != null) previewBitmap.recycle();
        super.onDestroy();
    }
}
