package com.printdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.support.annotation.RequiresApi;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Lalit on 14-Jun-17.
 */

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class MyPrintAdapter extends PrintDocumentAdapter {

    private Context context;
    private PrintedPdfDocument mPdfDocument;
    private int pages;

    public MyPrintAdapter(Context context) {
        this.context = context;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, Bundle extras) {
        // create new PDF with the requested page attributes
        mPdfDocument = new PrintedPdfDocument(context, newAttributes);

        // respond to cancellation request
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        // Prepare the layout.
        int newPageCount;
        // Mils is 1/1000th of an inch. Obviously.
        if (newAttributes.getMediaSize().getHeightMils() < 10000) {
            newPageCount = 2;
        } else {
            newPageCount = 1;
        }

        // Has the layout actually changed?
        boolean layoutChanged = newPageCount != pages;
        pages = newPageCount;

        if (pages > 0) {
            // return print information to print framework
            PrintDocumentInfo info = new PrintDocumentInfo
                    .Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
            // second argument indicates if layout has changed - if not cached one is used
            callback.onLayoutFinished(info, layoutChanged);
        } else {
            callback.onLayoutFailed("Page count calculation failed.");
        }
    }

    private int computePageCount(PrintAttributes printAttributes) {
        int itemsPerPage = 4; //default item count for portrait mode
        PrintAttributes.MediaSize pageSize = printAttributes.getMediaSize();
        if (!pageSize.isPortrait()) {
            itemsPerPage = 6; // 6 items per page in landscape mode
        }

        int printItemCount = getPrintItemCount();
        return (int) Math.ceil(printItemCount / itemsPerPage);
    }

    private int getPrintItemCount() {
        return 20;
    }

    private boolean containsPage(PageRange[] pages, int i) {
        for (PageRange range : pages) {
            if (i >= range.getStart() && i <= range.getEnd()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onWrite(final PageRange[] pageRanges, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        // Iterate over each page of the document,
        // check if it's in the output range.
        for (int i = 0; i < pages; i++) {
            // Check to see if this page is in the output range.
            if (containsPage(pageRanges, i)) {
                // If so, add it to writtenPagesArray. writtenPagesArray.size()
                // is used to compute the next output page index.
//                writtenPagesArray.append(writtenPagesArray.size(), i);
                PdfDocument.Page page = mPdfDocument.startPage(i);

                // check for cancellation
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    mPdfDocument.close();
                    mPdfDocument = null;
                    return;
                }

//                Rect imageRect = new Rect(10, 10, page.getCanvas().getWidth() - 10, page.getCanvas().getHeight() / 2 - 10);
//                drawImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher), page.getCanvas(), imageRect);
//                Rect imageRect1 = new Rect(10, page.getCanvas().getHeight() / 2 + 10, page.getCanvas().getWidth() - 10, page.getCanvas().getHeight() - 10);
//                drawImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher), page.getCanvas(), imageRect1);
//
//                Rect textRect = new Rect(10, page.getCanvas().getHeight() / 2 + 10, page.getCanvas().getWidth() - 10, page.getCanvas().getHeight() - 10);
//                drawText("This is the printing demo testing.", page.getCanvas(), textRect);

                // Draw page content for printing
                drawPage(page);

                // Rendering is complete, so page can be finalized.
                mPdfDocument.finishPage(page);
            }
        }

        // Write PDF document to file
        try {
            mPdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            mPdfDocument.close();
            mPdfDocument = null;
        }
        PageRange[] writtenPages = computeWrittenPages();
        // Signal the print framework the document is complete
        callback.onWriteFinished(writtenPages);

    }

    private PageRange[] computeWrittenPages() {

        PageRange[] pageRanges = new PageRange[1];

        pageRanges[0] = new PageRange(0, 1);

        // TODO Auto-generated method stub
        return pageRanges;
    }

    private void drawPage(PdfDocument.Page page) {
        Canvas canvas = page.getCanvas();

        // units are in points (1/72 of an inch)
        int titleBaseLine = 72;
        int leftMargin = 54;

        canvas.save();
        canvas.translate(0, 0);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(canvas.getWidth(), canvas.getHeight()));

        View view= LayoutInflater.from(context).inflate(R.layout.print_layout,null);
        view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

//        ((TextView)view.findViewById(R.id.print_text)).setText("This is demo text");

        frameLayout.addView(view);
        frameLayout.measure(canvas.getWidth() ,canvas.getHeight());
        frameLayout.layout(10, 10, 10,10);
        frameLayout.draw(canvas);
        canvas.restore();

//        Paint paint = new Paint();
//        paint.setColor(Color.BLACK);
//        paint.setTextSize(36);
//        canvas.drawText("Test Title", leftMargin, titleBaseLine, paint);
//
//        paint.setTextSize(11);
//        canvas.drawText("Test paragraph", leftMargin, titleBaseLine + 25, paint);
//
//        paint.setColor(Color.BLUE);
//        canvas.drawRect(100, 100, 172, 172, paint);
    }

    private void drawText(String text, Canvas canvas, Rect rect) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(20);
        paint.setColor(Color.BLACK);

        StaticLayout sl = new StaticLayout(text, paint, (int) rect.width(), Layout.Alignment.ALIGN_CENTER, 1, 1, false);

        canvas.save();
        canvas.translate(rect.left, rect.top);
        sl.draw(canvas);
        canvas.restore();
    }

    private void drawImage(Bitmap image, Canvas canvas, Rect r) {
        canvas.drawBitmap(image, null, r, new Paint());
    }
}
