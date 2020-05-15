package uibk.ac.at.prodigaclient.tests.MockCreators;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BluetoothDeviceMockCreator {
    private static final String FACETSERVICEUUID = "f1196f50-71a4-11e6-bdf4-0800200c9a66";
    private static final String BATTERYSERVICEUUID = "0000180f-0000-1000-8000-00805f9b34fb";

    private static final String BATTERYCHARACTERISTICUUID = "00002a19-0000-1000-8000-00805f9b34fb";
    private static final String CURRENTFACETCHARACTERISTICUUID = "f1196f52-71a4-11e6-bdf4-0800200c9a66";
    private static final String COMMANDREADCHARACTERISTICUUID = "f1196f53-71a4-11e6-bdf4-0800200c9a66";
    private static final String COMMANDWRITERCHARACTERISTICUUID = "f1196f54-71a4-11e6-bdf4-0800200c9a66";
    private static final String PASSWORDCHARACTERISTICUUID = "f1196f57-71a4-11e6-bdf4-0800200c9a66";

    private static final byte[] CUBEPASSWORD = {0x30, 0x30, 0x30, 0x30, 0x30, 0x30};
    private static final byte[] READHISTORYCMD = {0x01};
    private static final byte[] DELETEHISTORYCMD = {0x02};

    public static BluetoothDevice mockFullBluetoothDevice(String deviceMac, String deviceName, List<byte[]> historyEntries, byte[] facetId, byte[] batteryStatus, boolean connected) {
        BluetoothDevice bluetoothDevice = mock(BluetoothDevice.class);
        BluetoothGattService bluetoothBatteryService = mock(BluetoothGattService.class);
        BluetoothGattService bluetoothFacetService = mock(BluetoothGattService.class);

        BluetoothGattCharacteristic bluetoothBatteryCharacteristic = mock(BluetoothGattCharacteristic.class);
        BluetoothGattCharacteristic bluetoothCurrentFacetCharacteristics = mock(BluetoothGattCharacteristic.class);
        BluetoothGattCharacteristic bluetoothCommandWriteCharacteristics = mock(BluetoothGattCharacteristic.class);
        BluetoothGattCharacteristic bluetoothCommandReadCharacteristics = mock(BluetoothGattCharacteristic.class);
        BluetoothGattCharacteristic bluetoothPasswordCharacteristic = mock(BluetoothGattCharacteristic.class);

        when(bluetoothDevice.getName()).thenReturn(deviceName);
        when(bluetoothDevice.getAddress()).thenReturn(deviceMac);

        when(bluetoothDevice.connect()).thenReturn(true);
        when(bluetoothDevice.getConnected()).thenReturn(connected);
        when(bluetoothDevice.disconnect()).thenReturn(true);

        when(bluetoothDevice.find(eq(BATTERYSERVICEUUID), any(Duration.class))).thenReturn(bluetoothBatteryService);
        when(bluetoothDevice.find(eq(FACETSERVICEUUID), any(Duration.class))).thenReturn(bluetoothFacetService);

        when(bluetoothBatteryService.getUUID()).thenReturn(BATTERYSERVICEUUID);
        when(bluetoothFacetService.getUUID()).thenReturn(FACETSERVICEUUID);

        when(bluetoothFacetService.find(eq(COMMANDREADCHARACTERISTICUUID), any(Duration.class))).thenReturn(bluetoothCommandReadCharacteristics);
        when(bluetoothFacetService.find(eq(COMMANDWRITERCHARACTERISTICUUID), any(Duration.class))).thenReturn(bluetoothCommandWriteCharacteristics);
        when(bluetoothFacetService.find(eq(CURRENTFACETCHARACTERISTICUUID), any(Duration.class))).thenReturn(bluetoothCurrentFacetCharacteristics);
        when(bluetoothFacetService.find(eq(PASSWORDCHARACTERISTICUUID), any(Duration.class))).thenReturn(bluetoothPasswordCharacteristic);

        when(bluetoothBatteryService.find(eq(BATTERYCHARACTERISTICUUID), any(Duration.class))).thenReturn(bluetoothBatteryCharacteristic);

        when(bluetoothCommandReadCharacteristics.getUUID()).thenReturn(COMMANDREADCHARACTERISTICUUID);
        when(bluetoothCommandWriteCharacteristics.getUUID()).thenReturn(COMMANDWRITERCHARACTERISTICUUID);
        when(bluetoothCurrentFacetCharacteristics.getUUID()).thenReturn(CURRENTFACETCHARACTERISTICUUID);
        when(bluetoothPasswordCharacteristic.getUUID()).thenReturn(PASSWORDCHARACTERISTICUUID);
        when(bluetoothBatteryCharacteristic.getUUID()).thenReturn(BATTERYCHARACTERISTICUUID);

        when(bluetoothBatteryCharacteristic.readValue()).thenReturn(batteryStatus);

        when(bluetoothPasswordCharacteristic.writeValue(any(byte[].class))).thenAnswer(
                (Answer<Boolean>) invocationOnMock -> invocationOnMock.getArgument(0).equals(BluetoothDeviceMockCreator.CUBEPASSWORD)
        );

        when(bluetoothCommandWriteCharacteristics.writeValue(READHISTORYCMD)).thenReturn(true);
        when(bluetoothCommandWriteCharacteristics.writeValue(DELETEHISTORYCMD)).thenReturn(true);

        when(bluetoothCommandReadCharacteristics.readValue()).thenAnswer(new Answer<byte[]>() {
            int historyStatus = 0;
            final List<byte[]> historyEntryList = historyEntries;

            @Override
            public byte[] answer(InvocationOnMock invocationOnMock) {
                byte[] returnValue;
                returnValue = historyEntryList.get(historyStatus % historyEntryList.size());
                historyStatus = historyStatus + 1;

                return returnValue;
            }
        });



        when(bluetoothCurrentFacetCharacteristics.readValue()).thenReturn(facetId);
        return bluetoothDevice;
    }


    public static BluetoothDevice mockConnectionTestBluetoothDevice(boolean connectionStatus) {
        BluetoothDevice bluetoothDevice = mock(BluetoothDevice.class);

        when(bluetoothDevice.getName()).thenReturn("TimeFlip");
        when(bluetoothDevice.getAddress()).thenReturn("12:34:56:78:90:12");

        when(bluetoothDevice.connect()).thenReturn(true);
        when(bluetoothDevice.getConnected()).thenReturn(connectionStatus);
        when(bluetoothDevice.disconnect()).thenReturn(true);

        return bluetoothDevice;
    }

    public static BluetoothDevice mockBrokenConnectionTestBluetoothDevice(boolean connectionStatus) {
        BluetoothDevice bluetoothDevice = mock(BluetoothDevice.class);

        when(bluetoothDevice.getName()).thenReturn("TimeFlip");
        when(bluetoothDevice.getAddress()).thenReturn("12:34:56:78:90:12");

        when(bluetoothDevice.connect()).thenThrow(BluetoothException.class).thenThrow(BluetoothException.class).thenReturn(true);
        when(bluetoothDevice.getConnected()).thenReturn(connectionStatus);
        when(bluetoothDevice.disconnect()).thenThrow(BluetoothException.class).thenThrow(BluetoothException.class).thenReturn(true);

        return bluetoothDevice;
    }
}
