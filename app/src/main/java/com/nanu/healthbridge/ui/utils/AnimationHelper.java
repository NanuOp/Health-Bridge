package com.nanu.healthbridge.ui.utils;

import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import java.util.Locale;

public class AnimationHelper {

    public static void animateNumber(final TextView tv, int from, int to, int durationMs) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(durationMs);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> tv.setText(String.valueOf(animation.getAnimatedValue())));
        animator.start();
    }

    public static void animateNumberFloat(final TextView tv, float from, float to, int durationMs, final String format) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(durationMs);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            tv.setText(String.format(Locale.US, format, val));
        });
        animator.start();
    }
}
