package com.example.overlaydetector;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import androidx.core.app.NotificationCompat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureService extends Service {
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";

    private static final String NOTIF_CHANNEL_ID = "capture_channel";
    private static final int NOTIF_ID = 1;
    private static final long SEND_INTERVAL_MS = 250L;
    private static final int MAX_SEND_DIM = 640;
    private static final long TRACK_TTL_MS = 450L;
    private static final float IOU_MATCH = 0.35f;
    private static final float EMA_ALPHA = 0.35f;
    private static final float SHOW_SCORE = 0.55f;
    private static final float HIDE_SCORE = 0.40f;
    private static final long NSFW_COOLDOWN_MS = 5_000L;

    private MediaProjection projection;
    private ImageReader reader;
    private VirtualDisplay display;
    private OverlayController overlay;
    private Detector detector;
    private EventReporter reporter;
    private Handler handler;
    private DisplayMetrics metrics;
    private Bitmap captureBitmap;
    private int[] rowIntBuf;
    private int trackSeq = 1;
    private long lastSendAt;
    private long lastAppliedAt;
    private long lastNsfwEventAt;
    private String lastNsfwSignature;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final ExecutorService netExec = Executors.newSingleThreadExecutor();
    private final ArrayList<Track> tracks = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        reporter = new EventReporter(this);
        overlay = new OverlayController(this);
        detector = new Detector();
        metrics = getResources().getDisplayMetrics();

        HandlerThread thread = new HandlerThread("capture-thread");
        thread.start();
        handler = new Handler(thread.getLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationManager.getNotificationChannel(NOTIF_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(new NotificationChannel(
                        NOTIF_CHANNEL_ID,
                        "Capture",
                        NotificationManager.IMPORTANCE_LOW
                ));
            }
        }

        var notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("Мониторинг экрана")
                .setContentText("Фильтрация NSFW активна")
                .setSmallIcon(android.R.drawable.ic_secure)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        int code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        Intent data = getProjectionIntent(intent);
        if (code != Activity.RESULT_OK || data == null) {
            return START_NOT_STICKY;
        }
        startCapture(code, data);
        return START_STICKY;
    }

    @SuppressWarnings("deprecation")
    private Intent getProjectionIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        }
        return intent.getParcelableExtra(EXTRA_RESULT_DATA);
    }

    private void startCapture(int code, Intent data) {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = manager == null ? null : manager.getMediaProjection(code, data);
        if (projection == null) {
            stopSelf();
            return;
        }

        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                if (display != null) {
                    display.release();
                }
                if (reader != null) {
                    reader.setOnImageAvailableListener(null, null);
                }
                stopSelf();
            }
        }, handler);

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
        reader.setOnImageAvailableListener(imageReader -> {
            Image image = imageReader.acquireLatestImage();
            if (image == null) {
                return;
            }
            try {
                onFrame(image, width, height);
            } finally {
                image.close();
            }
        }, handler);

        display = projection.createVirtualDisplay(
                "ScreenCap",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),
                null,
                null
        );
    }

    private void onFrame(Image image, int screenW, int screenH) {
        long now = System.currentTimeMillis();
        overlay.updateRects(buildRectsFromTracks());

        if (now - lastSendAt < SEND_INTERVAL_MS) {
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }

        Bitmap bitmap = imageToReusableBitmap(image);
        if (bitmap == null) {
            busy.set(false);
            return;
        }

        Bitmap scaled = scaleForSend(bitmap, MAX_SEND_DIM);
        float scaleX = screenW / (float) scaled.getWidth();
        float scaleY = screenH / (float) scaled.getHeight();
        lastSendAt = now;

        netExec.execute(() -> {
            try {
                List<NsfwBox> boxes = detector.detect(scaled);
                maybeSendNsfwEvent(boxes);

                List<BoxF> mapped = new ArrayList<>();
                for (NsfwBox box : boxes) {
                    float left = box.getX() * scaleX;
                    float top = box.getY() * scaleY;
                    float width = box.getWidth() * scaleX;
                    float height = box.getHeight() * scaleY;
                    if (width > 1f && height > 1f) {
                        mapped.add(new BoxF(
                                left,
                                top,
                                left + width,
                                top + height,
                                box.getScore(),
                                box.getLabel()
                        ));
                    }
                }
                applyDetections(mapped, System.currentTimeMillis(), screenW, screenH);
            } finally {
                if (scaled != bitmap) {
                    scaled.recycle();
                }
                busy.set(false);
            }
        });
    }

    private void maybeSendNsfwEvent(List<NsfwBox> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastNsfwEventAt < NSFW_COOLDOWN_MS) {
            return;
        }

        NsfwBox top = null;
        for (NsfwBox box : boxes) {
            if (top == null || box.getScore() > top.getScore()) {
                top = box;
            }
        }
        if (top == null) {
            return;
        }

        String signature = top.getLabel() + ":" + (int) (top.getScore() * 100);
        if (signature.equals(lastNsfwSignature) && now - lastNsfwEventAt < NSFW_COOLDOWN_MS * 2) {
            return;
        }

        lastNsfwEventAt = now;
        lastNsfwSignature = signature;
        String message = "NSFW: " + top.getLabel() + " (score="
                + String.format(Locale.US, "%.2f", top.getScore()) + ")";
        netExec.execute(() -> reporter.sendEvent("NSFW_DETECTED", message));
    }

    private Bitmap imageToReusableBitmap(Image image) {
        if (image.getFormat() != PixelFormat.RGBA_8888 && image.getFormat() != ImageFormat.FLEX_RGBA_8888) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        if (pixelStride != 4) {
            return null;
        }

        if (captureBitmap == null
                || captureBitmap.getWidth() != width
                || captureBitmap.getHeight() != height
                || captureBitmap.isRecycled()) {
            captureBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        int intsPerRow = rowStride / 4;
        if (rowIntBuf == null || rowIntBuf.length < width) {
            rowIntBuf = new int[width];
        }

        var intBuffer = buffer.asIntBuffer();
        for (int y = 0; y < height; y++) {
            intBuffer.position(y * intsPerRow);
            intBuffer.get(rowIntBuf, 0, width);
            captureBitmap.setPixels(rowIntBuf, 0, width, 0, y, width, 1);
        }
        return captureBitmap;
    }

    private Bitmap scaleForSend(Bitmap src, int maxDim) {
        int width = src.getWidth();
        int height = src.getHeight();
        int maxSide = Math.max(width, height);
        if (maxSide <= maxDim) {
            return src;
        }
        float scale = maxDim / (float) maxSide;
        int nextWidth = Math.max(1, (int) (width * scale));
        int nextHeight = Math.max(1, (int) (height * scale));
        return Bitmap.createScaledBitmap(src, nextWidth, nextHeight, true);
    }

    private void applyDetections(List<BoxF> detections, long now, int screenW, int screenH) {
        List<BoxF> filtered = new ArrayList<>();
        for (BoxF detection : detections) {
            if (detection.score >= HIDE_SCORE) {
                filtered.add(detection);
            }
        }

        boolean[] used = new boolean[tracks.size()];
        for (BoxF detection : filtered) {
            int bestIndex = -1;
            float bestIou = 0f;
            for (int i = 0; i < tracks.size(); i++) {
                Track track = tracks.get(i);
                float iou = iou(track.l, track.t, track.r, track.b, detection.l, detection.t, detection.r, detection.b);
                if (iou > bestIou) {
                    bestIou = iou;
                    bestIndex = i;
                }
            }

            if (bestIndex >= 0 && bestIou >= IOU_MATCH) {
                Track track = tracks.get(bestIndex);
                used[bestIndex] = true;
                float alpha = EMA_ALPHA;
                track.l = lerp(track.l, detection.l, alpha);
                track.t = lerp(track.t, detection.t, alpha);
                track.r = lerp(track.r, detection.r, alpha);
                track.b = lerp(track.b, detection.b, alpha);
                track.score = Math.max(track.score * (1f - alpha), detection.score);
                track.label = detection.label;
                track.lastSeen = now;
                track.visible = track.visible || detection.score >= SHOW_SCORE;
            } else if (detection.score >= SHOW_SCORE) {
                tracks.add(new Track(
                        trackSeq++,
                        detection.l,
                        detection.t,
                        detection.r,
                        detection.b,
                        detection.score,
                        detection.label,
                        now,
                        true
                ));
            }
        }

        Iterator<Track> iterator = tracks.iterator();
        while (iterator.hasNext()) {
            Track track = iterator.next();
            long age = now - track.lastSeen;
            if (age > TRACK_TTL_MS) {
                iterator.remove();
                continue;
            }
            if (track.visible && track.score < HIDE_SCORE) {
                track.visible = false;
            }
            if (!track.visible && track.score >= SHOW_SCORE) {
                track.visible = true;
            }
            track.l = clampF(track.l, 0f, screenW);
            track.t = clampF(track.t, 0f, screenH);
            track.r = clampF(track.r, 0f, screenW);
            track.b = clampF(track.b, 0f, screenH);
            if (track.r <= track.l + 2f || track.b <= track.t + 2f) {
                iterator.remove();
            }
        }

        lastAppliedAt = now;
    }

    private List<OverlayRect> buildRectsFromTracks() {
        if (tracks.isEmpty()) {
            return List.of();
        }
        List<OverlayRect> out = new ArrayList<>(tracks.size());
        for (Track track : tracks) {
            if (!track.visible) {
                continue;
            }
            int width = (int) (track.r - track.l);
            int height = (int) (track.b - track.t);
            if (width <= 2 || height <= 2) {
                continue;
            }
            out.add(new OverlayRect(
                    track.id,
                    (int) track.l,
                    (int) track.t,
                    width,
                    height
            ));
        }
        return out;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float iou(float l1, float t1, float r1, float b1, float l2, float t2, float r2, float b2) {
        float il = Math.max(l1, l2);
        float it = Math.max(t1, t2);
        float ir = Math.min(r1, r2);
        float ib = Math.min(b1, b2);
        float iw = ir - il;
        float ih = ib - it;
        if (iw <= 0f || ih <= 0f) {
            return 0f;
        }
        float inter = iw * ih;
        float a1 = Math.max(0f, r1 - l1) * Math.max(0f, b1 - t1);
        float a2 = Math.max(0f, r2 - l2) * Math.max(0f, b2 - t2);
        float union = a1 + a2 - inter;
        if (union <= 0f) {
            return 0f;
        }
        return inter / union;
    }

    private float clampF(float value, float low, float high) {
        return Math.min(high, Math.max(low, value));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (reader != null) {
            reader.setOnImageAvailableListener(null, null);
        }
        if (display != null) {
            display.release();
        }
        if (projection != null) {
            projection.stop();
        }
        if (overlay != null) {
            overlay.clearAll();
        }
        netExec.shutdownNow();
        if (captureBitmap != null) {
            captureBitmap.recycle();
            captureBitmap = null;
        }
        super.onDestroy();
    }

    private static class BoxF {
        final float l;
        final float t;
        final float r;
        final float b;
        final float score;
        final String label;

        BoxF(float l, float t, float r, float b, float score, String label) {
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
            this.score = score;
            this.label = label;
        }
    }

    private static class Track {
        final int id;
        float l;
        float t;
        float r;
        float b;
        float score;
        String label;
        long lastSeen;
        boolean visible;

        Track(int id, float l, float t, float r, float b, float score, String label, long lastSeen, boolean visible) {
            this.id = id;
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
            this.score = score;
            this.label = label;
            this.lastSeen = lastSeen;
            this.visible = visible;
        }
    }
}
