import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.Pin;
import org.firmata4j.I2CDevice;
import org.firmata4j.ssd1306.SSD1306;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MinorProject {

    private static final int PumpPin = 2;
    private static final int SensorPin = 15;
    private static final byte OLEDI2CAddress = 0x3C;

    private static final double DRYValue = 3.3;
    private static final double MoistureThreshold = 3.0;
    private static final double WetValue = 2.6;

    public static void main(String[] args) throws IOException, InterruptedException {
        IODevice arduino = new FirmataDevice("/dev/cu.usbserial-0001");
        arduino.start();
        Thread.sleep(5000);
        arduino.ensureInitializationIsDone();

        Pin pump = arduino.getPin(PumpPin);
        Pin sensor = arduino.getPin(SensorPin);

        pump.setMode(Pin.Mode.OUTPUT);
        sensor.setMode(Pin.Mode.ANALOG);

        I2CDevice i2cDevice = arduino.getI2CDevice(OLEDI2CAddress);
        SSD1306 oledDisplay = new SSD1306(i2cDevice, SSD1306.Size.SSD1306_128_64);
        oledDisplay.init();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    double sensorReading = sensor.getValue() * (5.0 / 1023.0);
                    System.out.println("Current Moisture Reading = " + sensorReading);

                    oledDisplay.getCanvas().clear();
                    oledDisplay.getCanvas().drawString(0, 0, "Moisture: " + String.format("%.2f", sensorReading));

                    if (sensorReading > DRYValue) { // Soil is dry
                        System.out.println("State: DRY \nAction: WATERING the plant.\n");
                        oledDisplay.getCanvas().drawString(0, 16, "Soil: DRY");
                        oledDisplay.getCanvas().drawString(0, 32, "Watering: YES");
                        pump.setValue(1); // Turn the pump on
                    } else if (sensorReading <= WetValue) { // Soil is wet
                        System.out.println("State: WET \nAction: NOT WATERING.\n");
                        oledDisplay.getCanvas().drawString(0, 16, "Soil: WET");
                        oledDisplay.getCanvas().drawString(0, 32, "Watering: NO");
                        pump.setValue(0); // Turn the pump off
                    } else { // Moisture is within the threshold range
                        System.out.println("State: MOIST \nAction: NOT WATERING.\n");
                        oledDisplay.getCanvas().drawString(0, 16, "Soil: MOIST");
                        oledDisplay.getCanvas().drawString(0, 32, "Watering: NO");
                        pump.setValue(0); // Keep the pump off
                    }

                    oledDisplay.display();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);

        Thread.sleep(60000);
    }
}