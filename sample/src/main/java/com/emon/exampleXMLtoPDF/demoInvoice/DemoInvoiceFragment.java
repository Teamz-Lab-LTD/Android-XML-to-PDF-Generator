package com.emon.exampleXMLtoPDF.demoInvoice;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.emon.exampleXMLtoPDF.R;
import com.emon.exampleXMLtoPDF.dummyList.DummyContent;
import com.emon.exampleXMLtoPDF.dummyList.DummyItemRecyclerViewAdapter;
import com.gkemon.XMLtoPDF.PdfGenerator;
import com.gkemon.XMLtoPDF.PdfGeneratorListener;
import com.gkemon.XMLtoPDF.model.FailureResponse;
import com.gkemon.XMLtoPDF.model.SuccessResponse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DemoInvoiceFragment extends Fragment {

    private static final String TAG = "InvoiceFragment";
    //invoice views edit items
    TextView customer_shop_name_tv, customer_address_tv, customer_phone_tv,
            customer_order_date_tv, our_delivery_date_tv;
    ImageView ivLogo;

    private View finalInvoiceViewToPrint;

    public DemoInvoiceFragment() {
    }


    /**
     * IMPORTANT:: We just need to print a invoice_layout LinearLayout,Not the whole RelativeLayout
     * So we need to make a view something like "finalInvoiceViewToPrint" which will be print out ignoring
     * the rest part of the root layout.
     *
     * @param root the main view group which is holding the invoice layout. We need to make a
     *             final invoice view from it (View root) which will be print out ignoring the rest part.
     * @return the final invoice view generating from the root view ignoring rest part
     * ( generate_invoice_btn button because we don't want to print it.We just print the main part of
     * the invoice)
     */
    private View createInvoiceViewFromRootView(View root) {

        finalInvoiceViewToPrint = root.findViewById(R.id.invoice_layout);
        RecyclerView invoice_rv = finalInvoiceViewToPrint.findViewById(R.id.invoice_rv);
        customer_shop_name_tv = finalInvoiceViewToPrint.findViewById(R.id.customer_shop_name_tv);
        customer_address_tv = finalInvoiceViewToPrint.findViewById(R.id.customer_address_tv);
        customer_phone_tv = finalInvoiceViewToPrint.findViewById(R.id.customer_phone_tv);
        customer_order_date_tv = finalInvoiceViewToPrint.findViewById(R.id.customer_order_date_tv);
        our_delivery_date_tv = finalInvoiceViewToPrint.findViewById(R.id.our_delivery_date_tv);
        customer_shop_name_tv.setText(Html.fromHtml("<b>Name:</b> Demo shop name"));
        customer_address_tv.setText(Html.fromHtml("<b>Address:</b> " + "Demo shop address"));
        customer_phone_tv.setText(Html.fromHtml("<b>Cell No:</b> " + "1234567"));
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        final String orderDate = format.format(1617976800);
        final String deliveryDate = format.format(Calendar.getInstance().getTime());
        customer_order_date_tv.setText(Html.fromHtml("<b>Order Date:</b> " + orderDate));
        our_delivery_date_tv.setText(Html.fromHtml("<b>Delivery Date:</b> " + deliveryDate));
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        invoice_rv.setLayoutManager(layoutManager);
        ivLogo=finalInvoiceViewToPrint.findViewById(R.id.iv_logo);
        ivLogo.setImageResource(R.mipmap.ic_launcher);
        invoice_rv.setAdapter(new DummyItemRecyclerViewAdapter(DummyContent.ITEMS));

        return finalInvoiceViewToPrint;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_demo_invoice, container, false);
        Button generate_invoice_btn = root.findViewById(R.id.generate_invoice_btn);

        finalInvoiceViewToPrint = createInvoiceViewFromRootView(root);

        generate_invoice_btn.setOnClickListener(v -> {
           generatePdf();
        });
        return root;
    }

    public void generatePdf() {
        PdfGenerator.getBuilder()
                .setContext(requireActivity())
                .fromViewSource()
                .fromView(finalInvoiceViewToPrint)
                /* "fromLayoutXML()" takes array of layout resources.
                 * You can also invoke "fromLayoutXMLList()" method here which takes list of layout resources instead of array. */
                /* It takes default page size like A4,A5. You can also set custom page size in pixel
                 * by calling ".setCustomPageSize(int widthInPX, int heightInPX)" here. */
                .setFileName("demo-invoice")
                /* It is file name */
                .setFolderNameOrPath("demo-invoice-folder/")
                /* It is folder name. If you set the folder name like this pattern (FolderA/FolderB/FolderC), then
                 * FolderA creates first.Then FolderB inside FolderB and also FolderC inside the FolderB and finally
                 * the pdf file named "Test-PDF.pdf" will be store inside the FolderB. */
                .actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.OPEN)
                /* It true then the generated pdf will be shown after generated. */
                .build(new PdfGeneratorListener() {
                    @Override
                    public void onFailure(FailureResponse failureResponse) {
                        super.onFailure(failureResponse);
                        Log.d(TAG, "onFailure: " + failureResponse.getErrorMessage());
                        /* If pdf is not generated by an error then you will findout the reason behind it
                         * from this FailureResponse. */
                        //Toast.makeText(MainActivity.this, "Failure : "+failureResponse.getErrorMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(getContext(), "" + failureResponse.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void showLog(String log) {
                        super.showLog(log);
                        Log.d(TAG, "log: " + log);
                        /*It shows logs of events inside the pdf generation process*/
                    }

                    @Override
                    public void onStartPDFGeneration() {

                    }

                    @Override
                    public void onFinishPDFGeneration() {

                    }

                    @Override
                    public void onSuccess(SuccessResponse response) {
                        super.onSuccess(response);
                        /* If PDF is generated successfully then you will find SuccessResponse
                         * which holds the PdfDocument,File and path (where generated pdf is stored)*/
                        //Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Success: " + response.getPath());

                    }
                });

    }

}