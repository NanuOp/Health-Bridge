package com.nanu.healthbridge.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.nanu.healthbridge.db.HealthDay;

import java.util.ArrayList;
import java.util.List;

public class TrendBarView extends View {
    private List<HealthDay> data = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TrendBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(Color.parseColor("#8888AA"));
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        outlinePaint.setColor(Color.WHITE);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(4f);
    }

    public void setData(List<HealthDay> days) {
        this.data = days;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) return;

        int n = data.size();
        float width = getWidth();
        float height = getHeight();
        float padding = 40f;
        float barWidth = (width - (padding * (n + 1))) / n;
        float chartHeight = height - 60f; // Space for labels

        for (int i = 0; i < n; i++) {
            HealthDay day = data.get(i);
            float score = day.recoveryScore;
            float barHeight = (score / 100f) * chartHeight;
            
            float left = padding + i * (barWidth + padding);
            float top = chartHeight - barHeight;
            float right = left + barWidth;
            float bottom = chartHeight;

            barPaint.setColor(Color.parseColor(day.color != null ? day.color : "#B39DDB"));
            
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 10f, 10f, barPaint);

            // Today highlight
            if (i == n - 1) {
                canvas.drawRoundRect(rect, 10f, 10f, outlinePaint);
            }

            // Label (Day of week)
            String label = day.date.substring(day.date.length() - 2); // Show day of month
            canvas.drawText(label, left + barWidth / 2, height - 10f, textPaint);
        }
    }
}
