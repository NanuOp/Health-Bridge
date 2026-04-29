package com.nanu.healthbridge.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class SparklineView extends View {

    private Paint linePaint;
    private Path linePath;
    private List<Integer> data = new ArrayList<>();
    private int lineColor = 0xFF00FF88;

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        
        linePath = new Path();
    }

    public void setData(List<Integer> data, int color) {
        this.data = data;
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.size() < 2) return;

        float width = getWidth();
        float height = getHeight();
        float xStep = width / (data.size() - 1);
        
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int val : data) {
            if (val < min) min = val;
            if (val > max) max = val;
        }
        
        float range = max - min;
        if (range == 0) range = 1;

        linePath.reset();
        for (int i = 0; i < data.size(); i++) {
            float x = i * xStep;
            float y = height - ((data.get(i) - min) / range * height * 0.8f) - (height * 0.1f);
            
            if (i == 0) {
                linePath.moveTo(x, y);
            } else {
                // For a smoother look, we could use cubicTo, but let's start with lineTo
                linePath.lineTo(x, y);
            }
        }
        canvas.drawPath(linePath, linePaint);
    }
}
