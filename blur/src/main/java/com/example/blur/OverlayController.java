package com.example.overlaydetector;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class OverlayController {
    private final WindowManager windowManager;
    private final Context context;
    private final AtomicReference<List<OverlayRect>> rectsRef = new AtomicReference<>(List.of());

    private OverlayView view;
    private WindowManager.LayoutParams layoutParams;

    public OverlayController(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Activity.WINDOW_SERVICE);
    }

    public void updateRects(List<OverlayRect> rects) {
        ensure();
        rectsRef.set(rects);
        if (view != null) {
            view.postInvalidateOnAnimation();
        }
    }

    public void clearAll() {
        rectsRef.set(List.of());
        if (view != null) {
            try {
                windowManager.removeView(view);
            } catch (Exception ignored) {
            }
        }
        view = null;
        layoutParams = null;
    }

    private void ensure() {
        if (view != null) {
            return;
        }
        OverlayView overlayView = new OverlayView(context, rectsRef);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;
        windowManager.addView(overlayView, params);
        view = overlayView;
        layoutParams = params;
    }

    private int overlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private static class OverlayView extends View {
        private final AtomicReference<List<OverlayRect>> rectsRef;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        OverlayView(Context context, AtomicReference<List<OverlayRect>> rectsRef) {
            super(context);
            this.rectsRef = rectsRef;
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(255);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            List<OverlayRect> rects = rectsRef.get();
            if (rects.isEmpty()) {
                return;
            }
            for (OverlayRect rect : rects) {
                canvas.drawRect(
                        rect.getX(),
                        rect.getY(),
                        rect.getX() + rect.getWidth(),
                        rect.getY() + rect.getHeight(),
                        paint
                );
            }
        }
    }
}
