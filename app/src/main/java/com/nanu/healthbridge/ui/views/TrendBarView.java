package com.nanu.healthbridge.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class TrendBarView extends View {

    public static class TrendData {
        public int score;
        public String label;
        public int color;

        public TrendData(int score, String label, int color) {
            this.score = score;
            this.label = label;
            this.color = color;
        }
    }

    private List<TrendData> data = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private RectF rect;

    public TrendBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8888AA"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        rect = new RectF();
    }

    public void setData(List<TrendData> data) {
        this.data = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float barWidth = width / (data.size() * 1.5f);
        float spacing = (width - (barWidth * data.size())) / (data.size() + 1);

        float currentX = spacing;
        float chartHeight = height - 80f; // Space for labels

        for (TrendData d : data) {
            float barHeight = (d.score / 100f) * chartHeight;
            barPaint.setColor(d.color);
            
            rect.set(currentX, chartHeight - barHeight, currentX + barWidth, chartHeight);
            canvas.drawRoundRect(rect, 12f, 12f, barPaint);
            
            // Draw score above bar
            textPaint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(d.score), currentX + barWidth / 2, chartHeight - barHeight - 10f, textPaint);
            
            // Draw label below bar
            textPaint.setColor(Color.parseColor("#8888AA"));
            canvas.drawText(d.label, currentX + barWidth / 2, height - 10f, textPaint);
            
            currentX += barWidth + spacing;
        }
    }
}
