package com.firos.dianoraremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.util.Log;

/**
 * Controller class managing Android's {@link ConsumerIrManager} hardware operations.
 */
public class IrTransmitterManager {

    private static final String TAG = "IrTransmitterManager";

    public enum TransmissionStatus {
        SUCCESS,
        NO_HARDWARE_EMITTER,
        FREQUENCY_NOT_SUPPORTED,
        TRANSMISSION_ERROR
    }

    public static class TransmissionResult {
        private final TransmissionStatus status;
        private final String message;
        private final int commandCode;

        public TransmissionResult(TransmissionStatus status, String message, int commandCode) {
            this.status = status;
            this.message = message;
            this.commandCode = commandCode;
        }

        public TransmissionStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public int getCommandCode() {
            return commandCode;
        }

        public boolean isSuccess() {
            return status == TransmissionStatus.SUCCESS;
        }
    }

    private final ConsumerIrManager consumerIrManager;
    private final boolean hasHardwareEmitter;

    public IrTransmitterManager(Context context) {
        ConsumerIrManager manager = null;
        boolean emitterPresent = false;

        try {
            manager = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
            if (manager != null && manager.hasIrEmitter()) {
                emitterPresent = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ConsumerIrManager: " + e.getMessage());
        }

        this.consumerIrManager = manager;
        this.hasHardwareEmitter = emitterPresent;
    }

    /**
     * Checks if the device has a built-in Infrared emitter.
     */
    public boolean hasHardwareEmitter() {
        return hasHardwareEmitter;
    }

    /**
     * Checks if the hardware transmitter supports 38 kHz modulation.
     */
    public boolean is38KhzSupported() {
        if (!hasHardwareEmitter || consumerIrManager == null) {
            return false;
        }

        try {
            ConsumerIrManager.CarrierFrequencyRange[] ranges = consumerIrManager.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) {
                // Many devices return null or empty array but support standard 38kHz
                return true;
            }
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                if (range.getMinFrequency() <= NecIrEncoder.CARRIER_FREQUENCY_HZ &&
                    range.getMaxFrequency() >= NecIrEncoder.CARRIER_FREQUENCY_HZ) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not query carrier frequency ranges: " + e.getMessage());
            return true; // Fallback to attempting transmission
        }
        return false;
    }

    /**
     * Transmits a Dianora TV command via infrared pulse modulation.
     *
     * @param commandCode 8-bit command hex code
     * @return TransmissionResult indicating hardware execution outcome
     */
    public TransmissionResult sendCommand(int commandCode) {
        if (!hasHardwareEmitter || consumerIrManager == null) {
            String hex = String.format("0x%02X", commandCode);
            return new TransmissionResult(
                TransmissionStatus.NO_HARDWARE_EMITTER,
                "No IR blaster found. Signal " + hex + " simulated.",
                commandCode
            );
        }

        try {
            int[] pattern = NecIrEncoder.encodeDianoraCommand(DianoraKeyMap.HEADER_DIANORA, commandCode);
            consumerIrManager.transmit(NecIrEncoder.CARRIER_FREQUENCY_HZ, pattern);

            String hex = String.format("0x%02X", commandCode);
            return new TransmissionResult(
                TransmissionStatus.SUCCESS,
                "Transmitted command " + hex + " at 38 kHz",
                commandCode
            );
        } catch (Exception e) {
            Log.e(TAG, "Transmission failed: " + e.getMessage(), e);
            return new TransmissionResult(
                TransmissionStatus.TRANSMISSION_ERROR,
                "IR Transmission Error: " + e.getMessage(),
                commandCode
            );
        }
    }
}
