package com.firatnexus.ototiklayici;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Yüzen kontrol panelini ve hedef dairelerini gösterir, hedeflere dokunma hareketi gönderir.
 * Erişilebilirlik katmanı (TYPE_ACCESSIBILITY_OVERLAY) kullandığı için ayrıca
 * "diğer uygulamaların üzerinde göster" izni gerekmez.
 *
 * v1.1: rastgele bekleme sapması (tespit önleme), hedef sapması (piksel),
 * panelde canlı tıklama sayacı, sürükleme biter bitmez konum kaydı,
 * hızlı ayar karo entegrasyonu (QuickTileService).
 */
public class ClickService extends AccessibilityService {

    static ClickService instance;

    private static final int MAX_TARGETS = 20;
    private static final int TAP_MS = 20;
    private static final long MIN_DELAY = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private WindowManager wm;
    private SharedPreferences prefs;

    private LinearLayout panel;
    private WindowManager.LayoutParams panelLp;
    private ImageView playBtn;
    private View addBtn, removeBtn;
    private TextView counter;
    private final List<TextView> targets = new ArrayList<>();
    private final List<WindowManager.LayoutParams> targetLps = new ArrayList<>();

    private boolean multi;
    private long interval;
    private long jitter;        // bekleme sapması ±ms
    private long targetJitter;  // tıklama noktası sapması ±px
    private int repeat;

    private boolean running;
    private int index, cycle, clicks;

    // ------------------------------------------------------------------ yaşam döngüsü

    @Override
    protected void onServiceConnected() {
        instance = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { stopClicking(); }

    @Override
    public void onDestroy() {
        hidePanel();
        instance = null;
        super.onDestroy();
    }

    // ------------------------------------------------------------------ panel

    boolean isPanelShown() { return panel != null; }

    boolean isClicking() { return running; }

    void showPanel(boolean multi, long interval, long jitter, long targetJitter, int repeat) {
        hidePanel();
        this.multi = multi;
        this.interval = interval;
        this.jitter = jitter;
        this.targetJitter = targetJitter;
        this.repeat = repeat;

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_panel);
        int p = dp(6);
        panel.setPadding(p, p, p, p);

        panelLp = baseParams();
        panelLp.x = prefs.getInt("panel_x", dp(8));
        panelLp.y = prefs.getInt("panel_y", screenH() / 3);

        playBtn = iconButton(R.drawable.ic_play);
        panel.addView(playBtn);
        if (multi) {
            addBtn = iconButton(R.drawable.ic_add);
            removeBtn = iconButton(R.drawable.ic_remove);
            panel.addView(addBtn);
            panel.addView(removeBtn);
        }
        View closeBtn = iconButton(R.drawable.ic_close);
        panel.addView(closeBtn);

        // Canlı tıklama sayacı
        counter = new TextView(this);
        counter.setGravity(Gravity.CENTER);
        counter.setTextColor(Color.WHITE);
        counter.setTextSize(9);
        counter.setPadding(0, dp(3), 0, dp(1));
        counter.setText("0");
        panel.addView(counter);

        // Butonlar hem dokununca çalışır hem de basılı tutup sürükleyince paneli taşır
        draggable(playBtn, panel, panelLp, this::toggleClicking);
        if (multi) {
            draggable(addBtn, panel, panelLp, this::addTarget);
            draggable(removeBtn, panel, panelLp, this::removeTarget);
        }
        draggable(closeBtn, panel, panelLp, this::hidePanel);

        wm.addView(panel, panelLp);

        // Kayıtlı hedefleri yükle
        String saved = prefs.getString(multi ? "pos_multi" : "pos_single", "");
        if (!saved.isEmpty()) {
            for (String pair : saved.split(";")) {
                String[] xy = pair.split(",");
                if (xy.length == 2) {
                    try {
                        addTargetAt(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    } catch (NumberFormatException ignored) { }
                }
                if (!multi) break;
            }
        }
        if (targets.isEmpty()) addTarget();
    }

    void hidePanel() {
        stopClicking();
        if (panel == null) return;
        savePositions();
        for (View t : targets) safeRemove(t);
        targets.clear();
        targetLps.clear();
        safeRemove(panel);
        panel = null;
        counter = null;
    }

    private void savePositions() {
        if (panel == null || panelLp == null) return;
        StringBuilder sb = new StringBuilder();
        for (WindowManager.LayoutParams lp : targetLps) {
            if (sb.length() > 0) sb.append(';');
            sb.append(lp.x).append(',').append(lp.y);
        }
        prefs.edit()
                .putString(multi ? "pos_multi" : "pos_single", sb.toString())
                .putInt("panel_x", panelLp.x)
                .putInt("panel_y", panelLp.y)
                .apply();
    }

    // ------------------------------------------------------------------ hedefler

    private void addTarget() {
        if (targets.size() >= MAX_TARGETS) {
            Toast.makeText(this, R.string.max_targets, Toast.LENGTH_SHORT).show();
            return;
        }
        int size = dp(56);
        int n = targets.size();
        // Yeni hedefleri ekranın ortasına, hafif kaydırarak yerleştir
        int x = screenW() / 2 - size / 2 + (n % 4) * dp(20);
        int y = screenH() / 2 - size / 2 + (n / 4) * dp(20);
        addTargetAt(x, y);
    }

    @SuppressLint("SetTextI18n")
    private void addTargetAt(int x, int y) {
        if (running) return;
        TextView t = new TextView(this);
        t.setBackgroundResource(R.drawable.bg_target);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(Color.WHITE);
        t.setTextSize(18);
        t.setShadowLayer(3, 0, 0, Color.BLACK);
        t.setText(multi ? String.valueOf(targets.size() + 1) : "");

        WindowManager.LayoutParams lp = baseParams();
        lp.width = dp(56);
        lp.height = dp(56);
        lp.x = x;
        lp.y = y;
        draggable(t, t, lp, null);
        wm.addView(t, lp);
        targets.add(t);
        targetLps.add(lp);
    }

    private void removeTarget() {
        if (running || targets.size() <= 1) return;
        int last = targets.size() - 1;
        safeRemove(targets.remove(last));
        targetLps.remove(last);
    }

    /** Çalışırken hedefler dokunmaları geçirmeli ki gönderilen dokunuş alttaki uygulamaya ulaşsın. */
    private void setTargetsTouchable(boolean touchable) {
        for (int i = 0; i < targets.size(); i++) {
            WindowManager.LayoutParams lp = targetLps.get(i);
            if (touchable) lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            else lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            View t = targets.get(i);
            t.setAlpha(touchable ? 1f : 0.55f);
            wm.updateViewLayout(t, lp);
        }
    }

    // ------------------------------------------------------------------ tıklama döngüsü

    void toggleClicking() {
        if (running) stopClicking(); else startClicking();
    }

    private void startClicking() {
        if (targets.isEmpty()) return;
        running = true;
        index = 0;
        cycle = 0;
        clicks = 0;
        playBtn.setImageResource(R.drawable.ic_stop);
        if (addBtn != null) { addBtn.setAlpha(0.35f); removeBtn.setAlpha(0.35f); }
        setTargetsTouchable(false);
        handler.postDelayed(clickStep, 150);
    }

    void stopClicking() {
        handler.removeCallbacks(clickStep);
        if (!running) return;
        running = false;
        if (playBtn != null) playBtn.setImageResource(R.drawable.ic_play);
        if (addBtn != null) { addBtn.setAlpha(1f); removeBtn.setAlpha(1f); }
        if (panel != null) setTargetsTouchable(true);
    }

    private final Runnable clickStep = new Runnable() {
        @Override
        public void run() {
            if (!running || targets.isEmpty()) return;
            View t = targets.get(index);
            int[] loc = new int[2];
            t.getLocationOnScreen(loc);
            float cx = loc[0] + t.getWidth() / 2f;
            float cy = loc[1] + t.getHeight() / 2f;

            // Hedef sapması: tıklama noktası ±targetJitter piksel içinde rastgele
            if (targetJitter > 0) {
                cx += random.nextInt((int) targetJitter * 2 + 1) - targetJitter;
                cy += random.nextInt((int) targetJitter * 2 + 1) - targetJitter;
            }

            Path path = new Path();
            path.moveTo(cx, cy);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, TAP_MS))
                    .build();

            boolean sent = dispatchGesture(gesture, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription g) { scheduleNext(); }
                @Override public void onCancelled(GestureDescription g) { scheduleNext(); }
            }, null);
            if (!sent) stopClicking();
        }
    };

    private void scheduleNext() {
        if (!running) return;
        clicks++;
        index++;
        if (index >= targets.size()) {
            index = 0;
            cycle++;
            if (repeat > 0 && cycle >= repeat) {
                stopClicking();
                Toast.makeText(this, getString(R.string.finished, clicks), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // Rastgele sapma: bekleme süresi interval ± jitter aralığında
        long delay = interval;
        if (jitter > 0) {
            delay = interval - jitter + random.nextInt((int) jitter * 2 + 1);
        }
        if (delay < MIN_DELAY) delay = MIN_DELAY;
        if (counter != null) counter.setText(String.valueOf(clicks));
        handler.postDelayed(clickStep, Math.max(0, delay - TAP_MS));
    }

    // ------------------------------------------------------------------ yardımcılar

    private WindowManager.LayoutParams baseParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        return lp;
    }

    private ImageView iconButton(int icon) {
        ImageView b = new ImageView(this);
        b.setImageResource(icon);
        b.setBackgroundResource(R.drawable.bg_icon_btn);
        int s = dp(44), p = dp(10);
        b.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(0, dp(2), 0, dp(2));
        b.setLayoutParams(lp);
        return b;
    }

    /**
     * Dokunulan view'u sürükleyince {@code window} penceresini taşır; kısa dokunuşta {@code onTap} çalışır.
     * Sürükleme biter bitmez konum kaydedilir (servis aniden kapansa bile kayıp olmaz).
     */
    @SuppressLint("ClickableViewAccessibility")
    private void draggable(View touch, View window, WindowManager.LayoutParams lp, Runnable onTap) {
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        touch.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        dragging = false;
                        v.setPressed(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (!dragging && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                            dragging = true;
                            v.setPressed(false);
                        }
                        if (dragging) {
                            lp.x = startX + (int) dx;
                            lp.y = startY + (int) dy;
                            wm.updateViewLayout(window, lp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (dragging) savePositions();
                        else if (onTap != null) onTap.run();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        return true;
                }
                return false;
            }
        });
    }

    private void safeRemove(View v) {
        try { wm.removeView(v); } catch (Exception ignored) { }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int screenW() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        return m.widthPixels;
    }

    private int screenH() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        return m.heightPixels;
    }
}
