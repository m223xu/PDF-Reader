package com.example.pdfreader;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Observable;

class Memento {
    ArrayList<Pair<ArrayList<Pair<Float, Float>>,Paint>> recovery;
    int pageIndex;
    Memento(ArrayList<Pair<ArrayList<Pair<Float, Float>>,Paint>> recovery, int pageIndex) {
        this.recovery = recovery;
        this.pageIndex = pageIndex;
    }
}

enum Tool {
    Hand,
    Draw,
    Highlight,
    Erase
}

public class Model extends Observable {
    private static Model ourInstance = new Model();
    static Model getInstance() {
        return ourInstance;
    }

    private ArrayList<Bitmap> pages;
    ArrayList<ArrayList<Pair<ArrayList<Pair<Float, Float>>,Paint>>> dataSet; // records all existing paths' points
    volatile private LinkedList<Memento> undoStack;
    volatile private LinkedList<Memento> redoStack;
    volatile private int curPageIndex;
    private int lastPageIndex;
    private Tool curTool;
    private final Paint drawStyle;
    private final Paint highlightStyle;
    private Paint curStyle;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer pdfRenderer;
    PDFImage pageImage;
    private ArrayList<Pair<Float, Float>> points = null; // current drawing paths' points

    private Model() {
        pages = new ArrayList<>();
        dataSet = new ArrayList<>();
        undoStack = new LinkedList<>();
        redoStack = new LinkedList<>();
        curPageIndex = 0;
        lastPageIndex = 0;
        curTool = Tool.Hand;
        drawStyle = new Paint();
        drawStyle.setColor(Color.BLUE);
        drawStyle.setStrokeWidth(5);
        drawStyle.setStyle(Paint.Style.STROKE);
        highlightStyle = new Paint();
        highlightStyle.setColor(Color.argb(0.5f,1,1,0));
        highlightStyle.setStrokeWidth(20);
        highlightStyle.setStyle(Paint.Style.STROKE);
    }

    void addImage(PDFImage image) {
        pageImage = image;
    }

    int getCurPageIndex() {
        return curPageIndex;
    }

    int getTotalPages() {
        return pages.size();
    }

    Bitmap getCurrentPage() {
        return pages.get(curPageIndex);
    }

    ArrayList<Pair<ArrayList<Pair<Float,Float>>,Paint>> getCurrentPoints() {
        return dataSet.get(curPageIndex);
    }

    Tool getTool() {
        return curTool;
    }


    void setTool(Tool t) {
        curTool = t;
        if (curTool.equals(Tool.Draw)) {
            curStyle = drawStyle;
        }
        else if (curTool.equals(Tool.Highlight)) {
            curStyle = highlightStyle;
        }
        else curStyle = null;
    }

    void openRenderer(Context context) throws IOException {
        String FILENAME = "shannon1948.pdf";
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = context.getResources().openRawResource(R.raw.shannon1948);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }

        // convert to bitmaps
        for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
            PdfRenderer.Page currentPage = pdfRenderer.openPage(i);
            Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pages.add(bitmap);
            dataSet.add(new ArrayList<Pair<ArrayList<Pair<Float, Float>>, Paint>>());
            currentPage.close();
        }
    }

    void closeRenderer() throws IOException {
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    synchronized boolean canUndo() {
        return undoStack.size() > 0;
    }

    synchronized boolean canRedo() {
        return redoStack.size() > 0;
    }

    synchronized void undo() {
        if (canUndo()) {
            redoStack.addFirst(backup(lastPageIndex));
            recover(undoStack.pollFirst());
        }
    }

    synchronized void redo() {
        if (canRedo()) {
            undoStack.addFirst(backup(lastPageIndex));
            recover(redoStack.pollFirst());
        }
    }

    synchronized void prev() {
        if (curPageIndex > 0) {
            curPageIndex--;
        }
    }

    synchronized void next() {
        if (curPageIndex < pages.size() - 1) {
            curPageIndex++;
        }
    }

    synchronized private Memento backup(int backupPageIndex) {
        ArrayList<Pair<ArrayList<Pair<Float, Float>>, Paint>> backupState = new ArrayList<>();
        for (Pair<ArrayList<Pair<Float, Float>>, Paint> annotation : dataSet.get(backupPageIndex)) {
            ArrayList<Pair<Float, Float>> path = new ArrayList<>();
            for (Pair<Float, Float> point : annotation.first) {
                path.add(new Pair<>(point.first, point.second));
            }
            Pair<ArrayList<Pair<Float, Float>>, Paint> annotationCopy = new Pair<>(path, annotation.second);
            backupState.add(annotationCopy);
        }
        return new Memento(backupState, backupPageIndex);
    }

    synchronized private void recover(Memento memento) {
        ArrayList<Pair<ArrayList<Pair<Float, Float>>, Paint>> paths = new ArrayList<>();
        for (Pair<ArrayList<Pair<Float, Float>>, Paint> annotation : memento.recovery) {
            ArrayList<Pair<Float, Float>> path = new ArrayList<>();
            for (Pair<Float, Float> point : annotation.first) {
                path.add(new Pair<>(point.first, point.second));
            }
            Pair<ArrayList<Pair<Float, Float>>, Paint> annotationCopy = new Pair<>(path, annotation.second);
            paths.add(annotationCopy);
        }
        curPageIndex = memento.pageIndex;
        lastPageIndex = curPageIndex;
        dataSet.set(curPageIndex, paths);
    }

    synchronized void initPath(float x, float y) {
        undoStack.addFirst(backup(lastPageIndex));
        if (undoStack.size() > 10) {
            undoStack.removeLast();
        }
        redoStack.clear();

        if (lastPageIndex != curPageIndex) {
            undoStack.addFirst(backup(curPageIndex));
            if (undoStack.size() > 10) {
                undoStack.removeLast();
            }
            redoStack.clear();
        }

        points = new ArrayList<>();
        points.add(new Pair(x,y));
        getCurrentPoints().add(new Pair<>(points, curStyle));
        lastPageIndex = curPageIndex;
    }
    synchronized void updatePath(float x, float y) {
        points.add(new Pair(x,y));
    }
    synchronized void cleanupPath(float x, float y) {
        points = null;
    }

    boolean hit(Pair<Float, Float> p1, Pair<Float, Float> p2, Pair<Float, Float> test) {
        final float tolerance = 5f;
        float x = test.first, y = test.second;
        if (x > Math.max(p1.first, p2.first) + tolerance || x < Math.min(p1.first, p2.first) - tolerance) return false;
        float slope = (p2.first - p1.first == 0) ? Integer.MAX_VALUE : (p2.second - p1.second) / (p2.first - p1.first);
        float yEstimate = slope * (x - p1.first) + p1.second;
        return yEstimate - tolerance <= y && y <= yEstimate + tolerance;
    }

    synchronized void erase(float x, float y) {
        Pair<Float, Float> touch = new Pair<>(x, y);
        for (int i = getCurrentPoints().size() - 1; i >= 0; i--) {
            ArrayList<Pair<Float,Float>> path = getCurrentPoints().get(i).first;
            boolean hit = false;
            for (int j = 0; j < path.size() - 1; j++) {
                if (hit(path.get(j), path.get(j+1), touch)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                undoStack.addFirst(backup(lastPageIndex));
                if (undoStack.size() > 6) {
                    undoStack.removeLast();
                }
                redoStack.clear();

                if (lastPageIndex != curPageIndex) {
                    undoStack.addFirst(backup(curPageIndex));
                    if (undoStack.size() > 10) {
                        undoStack.removeLast();
                    }
                    redoStack.clear();
                }

                getCurrentPoints().remove(i);
                lastPageIndex = curPageIndex;
                break;
            }
        }
    }
}
