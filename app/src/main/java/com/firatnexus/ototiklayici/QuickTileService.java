package com.firatnexus.ototiklayici;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Hızlı ayarlar karosu: panel açıksa tıklamayı başlat/durdur,
 * kapalıysa uygulamayı açar. Karo, tıklama çalışırken yeşil yanar.
 */
public class QuickTileService extends TileService {

    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        ClickService s = ClickService.instance;
        if (s != null && s.isPanelShown()) {
            s.toggleClicking();
            update();
        } else {
            Intent in = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivityAndCollapse(in);
            } catch (Exception e) {
                try { startActivity(in); } catch (Exception ignored) { }
            }
        }
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        ClickService s = ClickService.instance;
        boolean active = s != null && s.isClicking();
        t.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(getString(R.string.tile_label));
        t.updateTile();
    }
}
