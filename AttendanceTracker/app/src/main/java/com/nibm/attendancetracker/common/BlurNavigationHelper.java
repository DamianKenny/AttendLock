package com.nibm.attendancetracker.common;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

public class BlurNavigationHelper {

    private Activity activity;
    private ScrollView scrollView;
    private View blurBackgroundView;
    private View fragmentContainer;
    private RenderScript renderScript;
    private boolean isBlurSupported = true;

    public BlurNavigationHelper(Activity activity, ScrollView scrollView, View blurBackgroundView, View fragmentContainer) {
        this.activity = activity;
        this.scrollView = scrollView;
        this.blurBackgroundView = blurBackgroundView;
        this.fragmentContainer = fragmentContainer;

        try {
            this.renderScript = RenderScript.create(activity);
        } catch (Exception e) {
            isBlurSupported = false;
            // Fallback to static blur effect
            setupStaticBlur();
        }

        setupScrollListener();
    }

    private void setupScrollListener() {
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    if (isBlurSupported) {
                        updateBlurBackground();
                    } else {
                        updateStaticBlur();
                    }
                }
            });
        }
    }

    private void updateBlurBackground() {
        if (fragmentContainer == null || blurBackgroundView == null) return;

        try {
            // Get the content behind the navigation
            Bitmap contentBitmap = captureContentBehindNavigation();

            if (contentBitmap != null) {
                // Apply blur effect
                Bitmap blurredBitmap = blurBitmap(contentBitmap, 15f);

                // Set as background with fade animation
                animateBlurBackground(blurredBitmap);

                // Clean up
                if (!contentBitmap.isRecycled()) {
                    contentBitmap.recycle();
                }
            }
        } catch (Exception e) {
            // Fallback to static effect if blur fails
            setupStaticBlur();
        }
    }

    private Bitmap captureContentBehindNavigation() {
        try {
            // Calculate the area behind the navigation bar
            int navTop = blurBackgroundView.getTop();
            int contentHeight = Math.min(200, fragmentContainer.getHeight()); // Limit capture area

            // Create bitmap of the content area
            Bitmap bitmap = Bitmap.createBitmap(
                    fragmentContainer.getWidth(),
                    contentHeight,
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);

            // Translate canvas to capture the area behind navigation
            canvas.translate(0, -Math.max(0, navTop - contentHeight));

            // Draw the fragment container content
            fragmentContainer.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap blurBitmap(Bitmap bitmap, float radius) {
        if (renderScript == null) return bitmap;

        try {
            // Create output bitmap
            Bitmap outputBitmap = Bitmap.createBitmap(bitmap);

            // Create RenderScript allocations
            Allocation input = Allocation.createFromBitmap(renderScript, bitmap);
            Allocation output = Allocation.createFromBitmap(renderScript, outputBitmap);

            // Create blur script
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            blurScript.setRadius(Math.min(25f, radius)); // Max radius is 25
            blurScript.setInput(input);
            blurScript.forEach(output);

            // Copy result to output bitmap
            output.copyTo(outputBitmap);

            // Clean up
            input.destroy();
            output.destroy();
            blurScript.destroy();

            return outputBitmap;
        } catch (Exception e) {
            return bitmap;
        }
    }

    private void animateBlurBackground(Bitmap blurredBitmap) {
        if (blurBackgroundView == null || blurredBitmap == null) return;

        BitmapDrawable blurredDrawable = new BitmapDrawable(activity.getResources(), blurredBitmap);

        // Animate alpha for smooth transition
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 0.7f);
        animator.setDuration(150);
        animator.addUpdateListener(animation -> {
            float alpha = (Float) animation.getAnimatedValue();
            blurredDrawable.setAlpha((int) (alpha * 255));
            blurBackgroundView.setBackground(blurredDrawable);
        });
        animator.start();
    }

    private void updateStaticBlur() {
        // Fallback: Update transparency based on scroll position
        if (scrollView != null && blurBackgroundView != null) {
            int scrollY = scrollView.getScrollY();
            float alpha = Math.min(0.8f, scrollY / 500f); // Increase transparency as user scrolls
            blurBackgroundView.setAlpha(0.3f + alpha * 0.5f);
        }
    }

    private void setupStaticBlur() {
        // Static glass effect when RenderScript is not available
        if (blurBackgroundView != null) {
            blurBackgroundView.setAlpha(0.4f);
        }
    }

    public void cleanup() {
        if (renderScript != null) {
            renderScript.destroy();
            renderScript = null;
        }
    }

    // Enhanced glass morphism effect with scroll-based transparency
    public void setupScrollBasedTransparency() {
        if (scrollView == null) return;

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();
            float maxScroll = 300f; // Maximum scroll distance for full effect

            // Calculate transparency based on scroll position
            float scrollRatio = Math.min(1f, scrollY / maxScroll);

            // Update navigation bar transparency
            float navAlpha = 0.85f + (scrollRatio * 0.10f); // From 85% to 95% opacity
            if (blurBackgroundView != null) {
                blurBackgroundView.setAlpha(navAlpha);
            }

            // Add subtle scale effect
            float scale = 1f - (scrollRatio * 0.02f); // Slight scale down when scrolling
            if (blurBackgroundView != null) {
                blurBackgroundView.setScaleX(scale);
                blurBackgroundView.setScaleY(scale);
            }
        });
    }
}
