package com.firos.dianoraremote;

/**
 * High-precision NEC Infrared protocol encoder for Android {@link android.hardware.ConsumerIrManager}.
 *
 * <h3>NEC Protocol Specification:</h3>
 * <ul>
 *   <li><b>Carrier Frequency:</b> 38,000 Hz (38 kHz), 1/3 duty cycle.</li>
 *   <li><b>Leader Code:</b> 9,000 µs pulse burst (mark) followed by 4,500 µs idle period (space).</li>
 *   <li><b>Bit '0':</b> 560 µs burst mark followed by 560 µs space (Total: 1.12 ms).</li>
 *   <li><b>Bit '1':</b> 560 µs burst mark followed by 1,690 µs space (Total: 2.25 ms).</li>
 *   <li><b>Stop Bit:</b> 560 µs trailing burst mark.</li>
 *   <li><b>Frame Structure (32 bits, LSB first):</b>
 *       [Address (8 bits)] + [Inverted Address (8 bits)] + [Command (8 bits)] + [Inverted Command (8 bits)]
 *   </li>
 * </ul>
 */
public final class NecIrEncoder {

    /** Standard NEC infrared carrier modulation frequency in Hertz. */
    public static final int CARRIER_FREQUENCY_HZ = 38000;

    // Timing durations in microseconds (µs)
    private static final int LEADER_MARK_US = 9000;
    private static final int LEADER_SPACE_US = 4500;
    private static final int BIT_MARK_US = 560;
    private static final int BIT_ZERO_SPACE_US = 560;
    private static final int BIT_ONE_SPACE_US = 1690;
    private static final int STOP_MARK_US = 560;

    // Total elements in raw pattern:
    // 2 (leader mark + space) + 32 * 2 (mark + space per bit) + 1 (stop mark) = 67 elements
    private static final int PATTERN_LENGTH = 67;

    private NecIrEncoder() {
        // Prevent instantiation of utility class
    }

    /**
     * Encodes a 32-bit frame into an alternating microsecond pulse duration pattern.
     *
     * @param byte0 First byte transmitted (Address LSB..MSB)
     * @param byte1 Second byte transmitted (Inverted Address LSB..MSB)
     * @param byte2 Third byte transmitted (Command LSB..MSB)
     * @param byte3 Fourth byte transmitted (Inverted Command LSB..MSB)
     * @return Integer array of 67 alternating mark and space microsecond intervals
     */
    public static int[] buildRawPattern(int byte0, int byte1, int byte2, int byte3) {
        int[] pattern = new int[PATTERN_LENGTH];
        int index = 0;

        // 1. Leader Code
        pattern[index++] = LEADER_MARK_US;
        pattern[index++] = LEADER_SPACE_US;

        // 2. Data Payload (4 bytes = 32 bits, transmitted LSB first)
        int[] payloadBytes = {byte0 & 0xFF, byte1 & 0xFF, byte2 & 0xFF, byte3 & 0xFF};

        for (int currentByte : payloadBytes) {
            for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                pattern[index++] = BIT_MARK_US;
                boolean isBitOne = ((currentByte >> bitIndex) & 0x01) == 1;
                pattern[index++] = isBitOne ? BIT_ONE_SPACE_US : BIT_ZERO_SPACE_US;
            }
        }

        // 3. Trailing Stop Bit
        pattern[index] = STOP_MARK_US;

        return pattern;
    }

    /**
     * Encodes a standard Dianora NEC frame using a 16-bit address header and 8-bit command.
     *
     * @param addressHeader 16-bit header (e.g., 0xFD02 -> Address=0x02, ~Address=0xFD)
     * @param commandCode   8-bit command code (e.g., 0x05 for Power)
     * @return Transmittable microsecond pattern array for ConsumerIrManager.transmit()
     */
    public static int[] encodeDianoraCommand(int addressHeader, int commandCode) {
        int addressByte = addressHeader & 0xFF;
        int invertedAddressByte = (addressHeader >> 8) & 0xFF;
        int commandByte = commandCode & 0xFF;
        int invertedCommandByte = (~commandCode) & 0xFF;

        return buildRawPattern(addressByte, invertedAddressByte, commandByte, invertedCommandByte);
    }

    /**
     * Builds a standard NEC Repeat Code pattern (used when holding down volume/channel buttons).
     * Leader: 9ms mark, 2.25ms space, 560µs stop mark.
     *
     * @return Integer array of 3 microsecond timings
     */
    public static int[] buildRepeatPattern() {
        return new int[]{9000, 2250, 560};
    }
}
