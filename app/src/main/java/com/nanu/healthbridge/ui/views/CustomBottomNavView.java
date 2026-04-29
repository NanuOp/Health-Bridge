package com.nanu.healthbridge.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.animation.OvershootInterpolator;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomBottomNavView extends BottomNavigationView {

    public interface OnItemSelectedListener {
        void onSelected(int id);
    }

    private OnItemSelectedListener externalListener;
    private Paint indicatorPaint;
    private RectF indicatorRect;
    private float currentX = 0;
    private float targetX = 0;

    public CustomBottomNavView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        super.setOnItemSelectedListener(item -> {
            animateIndicatorTo(item);
            if (externalListener != null) {
                externalListener.onSelected(item.getItemId());
            }
            return true;
        });
    }

    private void init() {
        setWillNotDraw(false);
        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setColor(0x3300FF88); // 20% opacity green
        indicatorRect = new RectF();
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.externalListener = listener;
    }

    private void animateIndicatorTo(MenuItem item) {
        int index = 0;
        int menuSize = getMenu().size();
        for (int i = 0; i < menuSize; i++) {
            if (getMenu().getItem(i).getItemId() == item.getItemId()) {
                index = i;
                break;
            }
        }
        
        float width = getWidth();
        float itemWidth = width / (menuSize > 0 ? menuSize : 1);
        targetX = index * itemWidth;

        ValueAnimator animator = ValueAnimator.ofFloat(currentX, targetX);
        animator.setDuration(400);
        animator.setInterpolator(new OvershootInterpolator(1.2f));
        animator.addUpdateListener(animation -> {
            currentX = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        int size = getMenu().size();
        if (size == 0) return;
        
        float itemWidth = width / size;
        float pillPadding = 16f;
        float pillHeight = getResources().getDisplayMetrics().density * 40;
        
        indicatorRect.set(currentX + pillPadding, (height - pillHeight) / 2, 
                         currentX + itemWidth - pillPadding, (height + pillHeight) / 2);
        
        canvas.drawRoundRect(indicatorRect, pillHeight / 2, pillHeight / 2, indicatorPaint);
    }
}
