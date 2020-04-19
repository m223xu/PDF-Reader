package com.example.pdfreader;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Model model;
    PDFImage pageImage;
    String LOGNAME = "MainActivity";

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getSupportActionBar().hide();

        model = Model.getInstance();
        pageImage = new PDFImage(this);
        model.addImage(pageImage);

        final ImageButton drawButton = findViewById(R.id.draw_button);
        final ImageButton highlightButton = findViewById(R.id.highlight_button);
        final ImageButton eraseButton = findViewById(R.id.erase_button);

        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
                if (v.isSelected()) {
                    highlightButton.setSelected(false);
                    eraseButton.setSelected(false);
                    model.setTool(Tool.Draw);
                }
                else {
                    model.setTool(Tool.Hand);
                }
            }
        });
        highlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
                if (v.isSelected()) {
                    drawButton.setSelected(false);
                    eraseButton.setSelected(false);
                    model.setTool(Tool.Highlight);
                }
                else {
                    model.setTool(Tool.Hand);
                }
            }
        });
        eraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
                if (v.isSelected()) {
                    drawButton.setSelected(false);
                    highlightButton.setSelected(false);
                    model.setTool(Tool.Erase);
                }
                else {
                    model.setTool(Tool.Hand);
                }
            }
        });

        LinearLayout layout = findViewById(R.id.pdf_layout);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // open and show a page
        try {
            model.openRenderer(this);
        } catch (IOException e) {
            Log.d("pdf_reader", "cannot open PDF");
        }

        TextView fileName = findViewById(R.id.file_name);
        String FILENAME = "shannon1948";
        fileName.setText(FILENAME);
        fileName.setTextSize(24f);
        Log.d("file name size: ", ""+fileName.getTextSize());

        TextView page = findViewById(R.id.page);
        int pageNumber = model.getCurPageIndex() + 1;
        int size = model.getTotalPages();
        page.setText("Page " + pageNumber + "/" + size);
    }

    @Override
    protected void onDestroy() {
        Log.d("onDestroy", "onDestroy method called");
        super.onDestroy();
        try {
            model.closeRenderer();
        } catch (IOException e) {
            Log.d("pdf_reader", "cannot close PDF");
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }



    public void undo(View view) {
        model.undo();
    }

    public void redo(View view) {
        model.redo();
    }

    @SuppressLint("SetTextI18n")
    public void prev(View view) {
        model.prev();
        TextView textView = findViewById(R.id.page);
        int pageNumber = model.getCurPageIndex() + 1;
        int size = model.getTotalPages();
        textView.setText("Page " + pageNumber + "/" + size);
    }

    @SuppressLint("SetTextI18n")
    public void next(View view) {
        model.next();
        TextView textView = findViewById(R.id.page);
        int pageNumber = model.getCurPageIndex() + 1;
        int size = model.getTotalPages();
        textView.setText("Page " + pageNumber + "/" + size);
    }
}
