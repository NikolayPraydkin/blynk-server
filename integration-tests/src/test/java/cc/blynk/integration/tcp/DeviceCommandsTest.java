package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.illegalCommandBody;
import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class DeviceCommandsTest extends SingleServerInstancePerTest {

    @Test
    public void testAddNewDevice() throws Exception {
        Device device0 = new Device();
        device0.id = 0;
        device0.name = "My Dashboard";
        device0.boardType = BoardType.ESP8266;
        device0.status = Status.ONLINE;

        Device device1 = new Device();
        device1.id = 1;
        device1.name = "My Device";
        device1.boardType = BoardType.ESP8266;
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);
    }

    @Test
    public void testUpdateExistingDevice() throws Exception {
        Device device0 = new Device();
        device0.id = 0;
        device0.name = "My Dashboard Updated";
        device0.boardType = BoardType.ESP8266;
        device0.status = Status.ONLINE;

        clientPair.appClient.updateDevice(1, device0);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();

        assertNotNull(devices);
        assertEquals(1, devices.length);

        assertEqualDevice(device0, devices[0]);
    }

    @Test
    public void testUpdateNonExistingDevice() throws Exception {
        Device device = new Device();
        device.id = 100;
        device.name = "My Dashboard Updated";
        device.boardType = BoardType.ESP8266;

        clientPair.appClient.updateDevice(1, device);
        clientPair.appClient.verifyResult(illegalCommandBody(1));
    }

    @Test
    public void testGetDevices() throws Exception {
        Device device0 = new Device();
        device0.id = 0;
        device0.name = "My Dashboard";
        device0.boardType = BoardType.ESP8266;
        device0.status = Status.ONLINE;

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(1, devices.length);

        assertEqualDevice(device0, devices[0]);
    }

    @Test
    public void testTokenNotUpdatedForExistingDevice() throws Exception {
        Device device0 = new Device();
        device0.id = 0;
        device0.name = "My Dashboard";
        device0.boardType = BoardType.ESP8266;
        device0.status = Status.ONLINE;

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(1, devices.length);

        assertEqualDevice(device0, devices[0]);
        String token = devices[0].token;

        device0.name = "My Dashboard UPDATED";
        device0.token = "123";

        clientPair.appClient.updateDevice(1, device0);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        devices = clientPair.appClient.parseDevices();

        assertNotNull(devices);
        assertEquals(1, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEquals("My Dashboard UPDATED", devices[0].name);
        assertEquals(token, devices[0].token);
    }


    @Test
    public void testDeletedNewlyAddedDevice() throws Exception {
        Device device0 = new Device();
        device0.id = 0;
        device0.name = "My Dashboard";
        device0.boardType = BoardType.ESP8266;
        device0.status = Status.ONLINE;

        Device device1 = new Device();
        device1.id = 1;
        device1.name = "My Device";
        device1.boardType = BoardType.ESP8266;
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(device1);
        device1 = clientPair.appClient.parseDevice();
        assertNotNull(device1);
        assertNotNull(device1.token);
        clientPair.appClient.verifyResult(createDevice(1, device1));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        clientPair.appClient.deleteDevice(1, device1.id);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        devices = clientPair.appClient.parseDevices();

        assertNotNull(devices);
        assertEquals(1, devices.length);

        assertEqualDevice(device0, devices[0]);
    }

    private static void assertEqualDevice(Device expected, Device real) {
        assertEquals(expected.boardType, real.boardType);
        assertNotNull(real.token);
        assertEquals(expected.status, real.status);
    }

}
