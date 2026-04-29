package com.nanu.healthbridge.ui;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.nanu.healthbridge.R;

public class CustomMarkerView extends MarkerView {

    private final TextView tvContent;
    private final String unit;

    public CustomMarkerView(Context context, String unit) {
        super(context, R.layout.custom_marker_view);
        this.unit = unit;
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e.getY() == (int) e.getY()) {
            tvContent.setText((int) e.getY() + " " + unit);
        } else {
            tvContent.setText(e.getY() + " " + unit);
        }
        super.refreshContent(e, highlight);
    }

    private MPPointF mOffset;

    @Override
    public MPPointF getOffset() {
        if (mOffset == null) {
            // center the marker horizontally and vertically
            mOffset = new MPPointF(-(getWidth() / 2f), -getHeight());
        }
        return mOffset;
    }
}
