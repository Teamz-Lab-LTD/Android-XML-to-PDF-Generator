package com.gkemon.XMLtoPDF;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.util.AtomicFile;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.gkemon.XMLtoPDF.model.FailureResponse;
import com.gkemon.XMLtoPDF.model.SuccessResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Android Views to PDF. Build on the main thread; file I/O runs on a worker. */
public class PdfGenerator {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    // Retained for source compatibility. New page dimensions use PDF points (1/72 inch).
    @Deprecated public static double postScriptThreshold = .75;
    @Deprecated public static final int a4HeightInPX = 3508, a4WidthInPX = 2480,
            a5HeightInPX = 1748, a5WidthInPX = 2480;
    @Deprecated public static int a4HeightInPostScript = 2631, a4WidthInPostScript = 1860;
    public static int AS_LIKE_XML_WIDTH = 0, AS_LIKE_XML_HEIGHT = 0;
    public static final String errorMessageOfXMLtoPDFLifecycleObserver = "PDF save cancelled.";

    public enum PageSize { A4, A5, AS_LIKE_XML }
    public enum ActionAfterPDFGeneration { OPEN, SHARE, NONE }
    public interface ContextStep { FromSourceStep setContext(ComponentActivity context); }
    public interface FromSourceStep {
        LayoutXMLSourceIntakeStep fromLayoutXMLSource();
        ViewIDSourceIntakeStep fromViewIDSource();
        ViewSourceIntakeStep fromViewSource();
    }
    public interface ViewSourceIntakeStep {
        FileNameStep fromView(View... views);
        FileNameStep fromViewList(List<View> views);
    }
    public interface LayoutXMLSourceIntakeStep {
        FileNameStep fromLayoutXML(Integer... layouts);
        FileNameStep fromLayoutXMLList(List<Integer> layouts);
    }
    public interface ViewIDSourceIntakeStep {
        FileNameStep fromViewID(Activity activity, Integer... ids);
        FileNameStep fromViewIDList(Activity activity, List<Integer> ids);
    }
    public interface PageSizeStep { FileNameStep setPageSize(PageSize size); }
    public interface FileNameStep extends PageSizeStep { Build setFileName(String name); }
    public interface Build {
        void build(PdfGeneratorListener listener);
        Build setFolderNameOrPath(String folder);
        /** Legacy README alias. Prefer setFolderNameOrPath. */
        default Build setFolderName(String folder) { return setFolderNameOrPath(folder); }
        Build actionAfterPDFGeneration(ActionAfterPDFGeneration action);
        Build savePDFSharedStorage(XmlToPDFLifecycleObserver observer);
        Build setLandscape(boolean landscape);
        Build setPageMargin(int points);
        Build setBackgroundColor(int color);
    }
    public static ContextStep getBuilder() { return new Builder(); }

    public static class Builder implements ContextStep, FromSourceStep, ViewSourceIntakeStep,
            LayoutXMLSourceIntakeStep, ViewIDSourceIntakeStep, FileNameStep, Build {
        private ComponentActivity context;
        private Activity sourceActivity;
        private List<View> views;
        private List<Integer> layouts, ids;
        private String name, folder;
        private PageSize size = PageSize.AS_LIKE_XML;
        private boolean landscape, used;
        private int margin, background = Color.WHITE;
        private ActionAfterPDFGeneration action = ActionAfterPDFGeneration.OPEN;
        private XmlToPDFLifecycleObserver observer;

        public FromSourceStep setContext(ComponentActivity value) { context = value; return this; }
        public LayoutXMLSourceIntakeStep fromLayoutXMLSource() { return this; }
        public ViewIDSourceIntakeStep fromViewIDSource() { return this; }
        public ViewSourceIntakeStep fromViewSource() { return this; }
        public FileNameStep fromView(View... value) { return fromViewList(value == null ? null : Arrays.asList(value)); }
        public FileNameStep fromViewList(List<View> value) { views = value == null ? null : new ArrayList<>(value); return this; }
        public FileNameStep fromLayoutXML(Integer... value) { return fromLayoutXMLList(value == null ? null : Arrays.asList(value)); }
        public FileNameStep fromLayoutXMLList(List<Integer> value) { layouts = value == null ? null : new ArrayList<>(value); return this; }
        public FileNameStep fromViewID(Activity activity, Integer... value) { return fromViewIDList(activity, value == null ? null : Arrays.asList(value)); }
        public FileNameStep fromViewIDList(Activity activity, List<Integer> value) { sourceActivity = activity; ids = value == null ? null : new ArrayList<>(value); return this; }
        public FileNameStep setPageSize(PageSize value) { size = value; return this; }
        public Build setFileName(String value) { name = value; return this; }
        public Build setFolderNameOrPath(String value) { folder = value; return this; }
        public Build setLandscape(boolean value) { landscape = value; return this; }
        public Build setPageMargin(int value) { margin = value; return this; }
        public Build setBackgroundColor(int value) { background = value; return this; }
        public Build actionAfterPDFGeneration(ActionAfterPDFGeneration value) { action = value; return this; }
        public Build savePDFSharedStorage(XmlToPDFLifecycleObserver value) { observer = value; return this; }

        @Override public void build(PdfGeneratorListener listener) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                MAIN.post(() -> fail(listener, new IllegalStateException("Build PDFs on the main thread.")));
                return;
            }
            PdfDocument document = null;
            boolean reserved = false;
            try {
                if (used) throw new IllegalStateException("Create a new builder for each PDF.");
                used = true;
                if (listener != null) listener.onStartPDFGeneration();
                if (context == null || context.isDestroyed()) throw new IllegalArgumentException("A live ComponentActivity is required.");
                if (size == null || action == null) throw new IllegalArgumentException("Page size and action must not be null.");
                String fileName = safeFileName(name);
                if (margin < 0) throw new IllegalArgumentException("Page margin must not be negative.");
                if (observer != null) { observer.reserve(); reserved = true; }
                List<View> content = resolveViews();
                if (content.isEmpty()) throw new IllegalArgumentException("Provide at least one layout or View.");
                document = new PdfDocument();
                int[] dimensions = render(document, content);
                File target = observer == null ? outputFile(context, folder, fileName)
                        : File.createTempFile("pending-", ".pdf", stageDirectory(context));
                PdfDocument ready = document;
                document = null;
                IO.execute(() -> {
                    Exception error = null;
                    AtomicFile atomic = new AtomicFile(target);
                    FileOutputStream out = null;
                    try { out = atomic.startWrite(); ready.writeTo(out); atomic.finishWrite(out); }
                    catch (Exception e) { error = e; atomic.failWrite(out); }
                    finally { ready.close(); }
                    Exception resultError = error;
                    MAIN.post(() -> {
                        if (resultError != null) {
                            if (observer != null) observer.release();
                            fail(listener, resultError);
                        } else if (observer != null) {
                            try { observer.save(target, fileName, dimensions, action, listener); }
                            catch (Exception e) { target.delete(); observer.release(); fail(listener, e); }
                        } else {
                            Uri uri;
                            try { uri = FileProvider.getUriForFile(context, context.getPackageName() + ".xmlToPdf.provider", target); }
                            catch (IllegalArgumentException e) { fail(listener, e); return; }
                            succeed(context, listener, target, uri, dimensions, action);
                        }
                    });
                });
            } catch (Exception e) {
                if (document != null) document.close();
                if (reserved) observer.release();
                fail(listener, e);
            }
        }

        private List<View> resolveViews() {
            List<View> result = new ArrayList<>();
            if (layouts != null) {
                FrameLayout parent = new FrameLayout(context);
                for (Integer id : layouts) {
                    if (id == null) throw new IllegalArgumentException("Layout ID must not be null.");
                    result.add(LayoutInflater.from(context).inflate(id, parent, false));
                }
            } else if (ids != null) {
                if (sourceActivity == null) throw new IllegalArgumentException("Source activity must not be null.");
                for (Integer id : ids) {
                    View view = id == null ? null : sourceActivity.findViewById(id);
                    if (view == null) throw new IllegalArgumentException("View ID not found: " + id);
                    result.add(view);
                }
            } else if (views != null) result.addAll(views);
            for (View view : result) if (view == null) throw new IllegalArgumentException("Views must not contain null.");
            return result;
        }

        private int[] render(PdfDocument doc, List<View> content) {
            int pageCount = 0, outputWidth = 0, outputHeight = 0;
            for (View view : content) {
                Rect old = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                int oldWidth = view.getMeasuredWidth(), oldHeight = view.getMeasuredHeight();
                try {
                    ViewGroup.LayoutParams lp = view.getLayoutParams();
                    int width = lp != null && lp.width > 0 ? lp.width : view.getWidth();
                    int height = lp != null && lp.height > 0 ? lp.height : 0;
                    if (width <= 0) {
                        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        width = view.getMeasuredWidth();
                    }
                    if (width <= 0) throw new IllegalArgumentException("View width is zero. Lay it out first or give it a fixed width.");
                    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            height > 0 ? View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY) : View.MeasureSpec.UNSPECIFIED);
                    height = view.getMeasuredHeight();
                    if (height <= 0) throw new IllegalArgumentException("View height is zero.");
                    view.layout(0, 0, width, height);
                    int pw = size == PageSize.A4 ? 595 : size == PageSize.A5 ? 420 : width;
                    int ph = size == PageSize.A4 ? 842 : size == PageSize.A5 ? 595 : height;
                    if (landscape && pw < ph) { int swap = pw; pw = ph; ph = swap; }
                    if ((long) margin * 2 >= Math.min(pw, ph)) throw new IllegalArgumentException("Margins leave no printable area.");
                    float scale = (pw - 2f * margin) / width;
                    int count = (int) Math.ceil(height * (double) scale / (ph - 2.0 * margin));
                    if (count > 1000 || pageCount + count > 1000) throw new IllegalArgumentException("PDF exceeds the 1000-page safety limit. Split the export.");
                    for (int slice = 0; slice < count; slice++) {
                        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(pw, ph, ++pageCount).create());
                        try {
                            Canvas canvas = page.getCanvas();
                            canvas.drawColor(background);
                            canvas.save();
                            canvas.clipRect(margin, margin, pw - margin, ph - margin);
                            canvas.translate(margin, margin - slice * (ph - 2f * margin));
                            canvas.scale(scale, scale);
                            view.draw(canvas);
                            canvas.restore();
                        } finally { doc.finishPage(page); }
                    }
                    outputWidth = pw; outputHeight = ph;
                } finally {
                    view.measure(View.MeasureSpec.makeMeasureSpec(oldWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(oldHeight, View.MeasureSpec.EXACTLY));
                    view.layout(old.left, old.top, old.right, old.bottom);
                    view.requestLayout();
                    view.invalidate();
                }
            }
            return new int[]{outputWidth, outputHeight, pageCount};
        }
    }

    static String safeFileName(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("File name must not be empty.");
        if (value.indexOf(0) >= 0) throw new IllegalArgumentException("File name must not contain a null byte.");
        String name = value.trim();
        if (name.contains("/") || name.contains("\\") || name.contains("\u0000") || name.equals(".") || name.equals(".."))
            throw new IllegalArgumentException("Use a file name, not a path.");
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") ? name : name + ".pdf";
    }
    static File outputFile(Context context, String folder, String name) throws IOException {
        File root = context.getExternalFilesDir(null);
        if (root == null) root = context.getFilesDir();
        File directory = folder == null || folder.trim().isEmpty() ? root : new File(folder);
        if (!directory.isAbsolute()) directory = new File(root, folder);
        String actual = directory.getCanonicalPath();
        boolean allowed = within(actual, context.getFilesDir()) || within(actual, context.getExternalFilesDir(null));
        if (!allowed) throw new IOException("Use an app-specific folder, or savePDFSharedStorage for a user-selected location.");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create PDF folder.");
        return new File(directory, name);
    }
    private static boolean within(String path, File root) throws IOException {
        return root != null && (path.equals(root.getCanonicalPath()) || path.startsWith(root.getCanonicalPath() + File.separator));
    }
    private static File stageDirectory(Context context) throws IOException {
        File dir = new File(context.getCacheDir(), "xml-to-pdf");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create PDF cache.");
        return dir;
    }
    private static void fail(PdfGeneratorListener listener, Exception error) {
        if (listener != null) {
            try { listener.onFailure(new FailureResponse(error)); }
            finally { listener.onFinishPDFGeneration(); }
        }
    }
    private static void succeed(Context context, PdfGeneratorListener listener, File file, Uri uri,
                                int[] dimensions, ActionAfterPDFGeneration action) {
        try {
            if (action != ActionAfterPDFGeneration.NONE) {
                Intent intent = createPdfIntent(uri, action);
                try { context.startActivity(action == ActionAfterPDFGeneration.SHARE ? Intent.createChooser(intent, "Share PDF") : intent); }
                catch (android.content.ActivityNotFoundException e) {
                    if (listener != null) listener.showLog("PDF saved, but no PDF viewer is installed. Use response.getUri().");
                }
            }
            if (listener != null) listener.onSuccess(new SuccessResponse(null, file, uri, dimensions[0], dimensions[1], dimensions[2]));
        } finally { if (listener != null) listener.onFinishPDFGeneration(); }
    }
    static Intent createPdfIntent(Uri uri, ActionAfterPDFGeneration action) {
        Intent intent = new Intent(action == ActionAfterPDFGeneration.SHARE ? Intent.ACTION_SEND : Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setClipData(ClipData.newRawUri("PDF", uri));
        if (action == ActionAfterPDFGeneration.SHARE) intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }
    interface PDFSaveListener { void onBlankPDFCreatedInSharedStorage(@Nullable Uri uri); }

    /** Register in onCreate. Reattach a result listener after recreation with setResultListener. */
    public static class XmlToPDFLifecycleObserver implements DefaultLifecycleObserver {
        private final ComponentActivity activity;
        private final PendingSave state;
        private ActivityResultLauncher<Intent> launcher;
        private PdfGeneratorListener listener;
        private PDFSaveListener legacyListener;
        private boolean destroyed;
        public XmlToPDFLifecycleObserver(@NonNull ComponentActivity activity) {
            this.activity = activity;
            state = new ViewModelProvider(activity).get(PendingSave.class);
        }
        public void setResultListener(PdfGeneratorListener listener) { this.listener = listener; }
        public void setPdfSaveListener(PDFSaveListener listener) { legacyListener = listener; }
        @Override public void onCreate(@NonNull LifecycleOwner owner) {
            state.observer = this;
            launcher = activity.getActivityResultRegistry().register("saved-pdf-from-xml", owner,
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        Uri uri = result.getResultCode() == Activity.RESULT_OK && result.getData() != null ? result.getData().getData() : null;
                        if (state.file != null) copyResult(uri);
                        else if (legacyListener != null) legacyListener.onBlankPDFCreatedInSharedStorage(uri);
                    });
        }
        @Override public void onDestroy(@NonNull LifecycleOwner owner) {
            destroyed = true;
            listener = null;
            legacyListener = null;
            if (state.observer == this) state.observer = null;
        }
        private void reserve() {
            if (destroyed || launcher == null) throw new IllegalStateException("Register XmlToPDFLifecycleObserver in Activity.onCreate before exporting.");
            if (state.busy) throw new IllegalStateException("A PDF save is already in progress.");
            state.busy = true;
        }
        private void release() { state.clearFile(); }
        private void save(File file, String name, int[] dimensions, ActionAfterPDFGeneration action, PdfGeneratorListener listener) {
            if (destroyed) throw new IllegalStateException("Activity changed before the save picker opened. Please export again.");
            state.file = file; state.dimensions = dimensions; state.action = action;
            this.listener = listener;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/pdf")
                    .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, name);
            launcher.launch(intent);
        }
        public void launchPDFSaverPicker(Intent intent) {
            if (launcher == null) throw new IllegalStateException("Register the observer in onCreate first.");
            launcher.launch(intent);
        }
        private void copyResult(Uri uri) {
            if (uri == null) {
                state.clearFile();
                fail(listener, new java.util.concurrent.CancellationException(errorMessageOfXMLtoPDFLifecycleObserver));
                return;
            }
            File file = state.file;
            Context app = activity.getApplicationContext();
            IO.execute(() -> {
                Exception error = null;
                try (FileInputStream in = new FileInputStream(file);
                     OutputStream out = app.getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new IOException("The document provider did not open an output stream.");
                    byte[] bytes = new byte[8192]; int count;
                    while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
                } catch (Exception e) { error = e; }
                Exception failure = error;
                MAIN.post(() -> {
                    XmlToPDFLifecycleObserver current = state.observer;
                    int[] dimensions = state.dimensions;
                    ActionAfterPDFGeneration action = state.action;
                    state.clearFile();
                    if (current != null) {
                        if (failure != null) fail(current.listener, failure);
                        else succeed(current.activity, current.listener, null, uri, dimensions, action);
                    }
                });
            });
        }
    }
    public static class PendingSave extends ViewModel {
        File file;
        int[] dimensions;
        ActionAfterPDFGeneration action;
        boolean busy;
        XmlToPDFLifecycleObserver observer;
        void clearFile() { if (file != null) file.delete(); file = null; busy = false; }
        @Override protected void onCleared() { clearFile(); observer = null; }
    }
}
