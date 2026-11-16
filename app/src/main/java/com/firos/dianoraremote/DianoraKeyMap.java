package com.firos.dianoraremote;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable keycode registry for Dianora Smart TVs (Platform: MT5862 / TP.SK518).
 * 
 * All command codes use the NEC Infrared protocol with Address Header 0xFD02.
 * Hardware Address: 0x02, Inverted Address: 0xFD.
 */
public final class DianoraKeyMap {

    /**
     * Dianora NEC 16-bit Address Header.
     * Byte 0 (Address) = 0x02, Byte 1 (~Address) = 0xFD.
     */
    public static final int HEADER_DIANORA = 0xFD02;

    // Power and System Keys
    public static final int KEY_POWER       = 0x05;
    public static final int KEY_INPUT       = 0x1F; // Source selection
    public static final int KEY_MUTE        = 0x46;
    public static final int KEY_SLEEP       = 0x01;
    public static final int KEY_ECO         = 0x55;

    // Navigation and D-Pad Controls
    public static final int KEY_UP          = 0x48;
    public static final int KEY_DOWN        = 0x40;
    public static final int KEY_LEFT        = 0x47;
    public static final int KEY_RIGHT       = 0x04;
    public static final int KEY_OK          = 0x44;
    public static final int KEY_BACK        = 0x56;
    public static final int KEY_HOME        = 0x0D;
    public static final int KEY_MENU        = 0x5F;
    public static final int KEY_SETTINGS    = 0x27;

    // Audio & Channel Controls
    public static final int KEY_VOL_UP      = 0x17;
    public static final int KEY_VOL_DOWN    = 0x57;
    public static final int KEY_CH_UP       = 0x48; // Mirrored on firmware
    public static final int KEY_CH_DOWN     = 0x40;

    // Media & Playback Controls
    public static final int KEY_PLAY_PAUSE  = 0x06;
    public static final int KEY_STOP        = 0x07;
    public static final int KEY_PREVIOUS    = 0x0F;
    public static final int KEY_NEXT        = 0x03;
    public static final int KEY_FAST_FWD    = 0x08;

    // Dedicated Color Keys
    public static final int KEY_RED         = 0x05; // Power / Red function
    public static final int KEY_GREEN       = 0x01; // Sleep / Green function
    public static final int KEY_YELLOW      = 0x14; // Picture Mode
    public static final int KEY_BLUE        = 0x50; // Sound Mode / Audio

    // Numeric Keypad (0-9)
    public static final int KEY_1           = 0x5E;
    public static final int KEY_2           = 0x5D;
    public static final int KEY_3           = 0x1E;
    public static final int KEY_4           = 0x4A;
    public static final int KEY_5           = 0x49;
    public static final int KEY_6           = 0x0A;
    public static final int KEY_7           = 0x5A;
    public static final int KEY_8           = 0x59;
    public static final int KEY_9           = 0x1A;
    public static final int KEY_0           = 0x4D;

    // Smart & Utility Functions
    public static final int KEY_INFO        = 0x16;
    public static final int KEY_MOUSE       = 0x02; // Fly mouse / pointer toggle
    public static final int KEY_ZOOM        = 0x11;
    public static final int KEY_FAVORITES   = 0x2A;

    private static final Map<String, Integer> CODE_MAP;

    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("POWER", KEY_POWER);
        map.put("INPUT", KEY_INPUT);
        map.put("MUTE", KEY_MUTE);
        map.put("SLEEP", KEY_SLEEP);
        map.put("ECO", KEY_ECO);

        map.put("UP", KEY_UP);
        map.put("DOWN", KEY_DOWN);
        map.put("LEFT", KEY_LEFT);
        map.put("RIGHT", KEY_RIGHT);
        map.put("OK", KEY_OK);
        map.put("BACK", KEY_BACK);
        map.put("HOME", KEY_HOME);
        map.put("MENU", KEY_MENU);
        map.put("SET", KEY_SETTINGS);
        map.put("SETTINGS", KEY_SETTINGS);

        map.put("VOL_UP", KEY_VOL_UP);
        map.put("VOL_DOWN", KEY_VOL_DOWN);
        map.put("CH_UP", KEY_CH_UP);
        map.put("CH_DOWN", KEY_CH_DOWN);

        map.put("PLAY_PAUSE", KEY_PLAY_PAUSE);
        map.put("STOP", KEY_STOP);
        map.put("PREV", KEY_PREVIOUS);
        map.put("NEXT", KEY_NEXT);
        map.put("FF", KEY_FAST_FWD);

        map.put("RED", KEY_RED);
        map.put("GREEN", KEY_GREEN);
        map.put("YELLOW", KEY_YELLOW);
        map.put("BLUE", KEY_BLUE);

        map.put("1", KEY_1);
        map.put("2", KEY_2);
        map.put("3", KEY_3);
        map.put("4", KEY_4);
        map.put("5", KEY_5);
        map.put("6", KEY_6);
        map.put("7", KEY_7);
        map.put("8", KEY_8);
        map.put("9", KEY_9);
        map.put("0", KEY_0);

        map.put("INFO", KEY_INFO);
        map.put("MOUSE", KEY_MOUSE);
        map.put("ZOOM", KEY_ZOOM);
        map.put("FAV", KEY_FAVORITES);

        CODE_MAP = Collections.unmodifiableMap(map);
    }

    private DianoraKeyMap() {
        // Prevent instantiation of utility class
    }

    /**
     * Resolves an alphanumeric key name to its verified Dianora NEC command code.
     *
     * @param keyName Identifier matching button tags in the UI
     * @return Integer command code (0x00..0xFF), or null if unrecognized
     */
    public static Integer getCommandCode(String keyName) {
        if (keyName == null) {
            return null;
        }
        return CODE_MAP.get(keyName.trim().toUpperCase());
    }

    /**
     * Returns whether the given key name is a recognized remote key.
     */
    public static boolean hasKey(String keyName) {
        return keyName != null && CODE_MAP.containsKey(keyName.trim().toUpperCase());
    }
}
