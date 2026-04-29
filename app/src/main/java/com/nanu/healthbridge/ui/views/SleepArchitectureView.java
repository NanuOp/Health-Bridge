package com.nanu.healthbridge.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class SleepArchitectureView extends View {

    public static class SleepSegment {
        public int status; // 1: Deep, 2: Light, 4: REM, others: Awake
        public int durationMinutes;

        public SleepSegment(int status, int durationMinutes) {
            this.status = status;
            this.durationMinutes = durationMinutes;
        }
    }

    private List<SleepSegment> segments = new ArrayList<>();
    private Paint paint;
    private RectF rect;

    public SleepArchitectureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        rect = new RectF();
    }

    public void setSegments(List<SleepSegment> segments) {
        this.segments = segments;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (segments == null || segments.isEmpty()) return;

        int totalMinutes = 0;
        for (SleepSegment s : segments) totalMinutes += s.durationMinutes;
        if (totalMinutes == 0) return;

        float width = getWidth();
        float height = getHeight();
        float currentX = 0;

        for (SleepSegment s : segments) {
            float segmentWidth = (s.durationMinutes / (float) totalMinutes) * width;
            
            // Status colors matching our Biopunk palette
            switch (s.status) {
                case 1: paint.setColor(0xFF7B61FF); break; // Deep
                case 4: paint.setColor(0xFF00D4FF); break; // REM
                case 2: paint.setColor(0xFF4466FF); break; // Light
                default: paint.setColor(0xFFFF3B5C); break; // Awake
            }

            rect.set(currentX, 0, currentX + segmentWidth, height);
            canvas.drawRoundRect(rect, 8f, 8f, paint);
            
            currentX += segmentWidth;
        }
    }
}
