package com.firatnexus.ototiklayici;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String PREFS = "ayarlar";

    private TextView permText, modeDesc;
    private Button permBtn, startBtn;
    private RadioGroup modeGroup;
    private EditText intervalInput, jitterInput, targetJitterInput, repeatInput;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        permText = findViewById(R.id.permText);
        permBtn = findViewById(R.id.permBtn);
        modeGroup = findViewById(R.id.modeGroup);
        modeDesc = findViewById(R.id.modeDesc);
        intervalInput = findViewById(R.id.intervalInput);
        jitterInput = findViewById(R.id.jitterInput);
        targetJitterInput = findViewById(R.id.targetJitterInput);
        repeatInput = findViewById(R.id.repeatInput);
        startBtn = findViewById(R.id.startBtn);

        boolean multi = prefs.getBoolean("multi", false);
        modeGroup.check(multi ? R.id.modeMulti : R.id.modeSingle);
        updateModeDesc();
        modeGroup.setOnCheckedChangeListener((g, id) -> updateModeDesc());
        intervalInput.setText(String.valueOf(prefs.getLong("interval", 100)));
        jitterInput.setText(String.valueOf(prefs.getLong("jitter", 0)));
        targetJitterInput.setText(String.valueOf(prefs.getLong("targetJitter", 0)));
        repeatInput.setText(String.valueOf(prefs.getInt("repeat", 0)));

        permBtn.setOnClickListener(v -> openAccessibilitySettings());
        startBtn.setOnClickListener(v -> togglePanel());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean on = ClickService.instance != null;
        permText.setText(on ? R.string.perm_ok : R.string.perm_missing);
        permText.setTextColor(getColor(on ? R.color.ok : R.color.bad));
        permBtn.setVisibility(on ? Button.GONE : Button.VISIBLE);
        boolean shown = on && ClickService.instance.isPanelShown();
        startBtn.setText(shown ? R.string.hide_panel : R.string.show_panel);
    }

    private void updateModeDesc() {
        modeDesc.setText(modeGroup.getCheckedRadioButtonId() == R.id.modeMulti
                ? R.string.mode_multi_desc : R.string.mode_single_desc);
    }

    private void openAccessibilitySettings() {
        Toast.makeText(this, R.string.perm_hint, Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void togglePanel() {
        ClickService s = ClickService.instance;
        if (s == null) {
            Toast.makeText(this, R.string.need_perm, Toast.LENGTH_SHORT).show();
            openAccessibilitySettings();
            return;
        }
        if (s.isPanelShown()) {
            s.hidePanel();
            refresh();
            return;
        }
        long interval = parseLong(intervalInput.getText().toString(), 100);
        if (interval < 10) interval = 10;
        long jitter = parseLong(jitterInput.getText().toString(), 0);
        if (jitter < 0) jitter = 0;
        if (jitter > interval / 2) jitter = interval / 2; // bekleme süresi negatife düşmesin
        long targetJitter = parseLong(targetJitterInput.getText().toString(), 0);
        if (targetJitter < 0) targetJitter = 0;
        if (targetJitter > 100) targetJitter = 100;
        int repeat = (int) parseLong(repeatInput.getText().toString(), 0);
        if (repeat < 0) repeat = 0;
        boolean multi = modeGroup.getCheckedRadioButtonId() == R.id.modeMulti;

        prefs.edit().putBoolean("multi", multi).putLong("interval", interval)
                .putLong("jitter", jitter).putLong("targetJitter", targetJitter)
                .putInt("repeat", repeat).apply();
        s.showPanel(multi, interval, jitter, targetJitter, repeat);
        moveTaskToBack(true);
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }
}
