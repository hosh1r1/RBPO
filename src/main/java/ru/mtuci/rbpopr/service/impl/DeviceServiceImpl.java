package ru.mtuci.rbpopr.service.impl;

import org.springframework.stereotype.Service;
import ru.mtuci.rbpopr.model.ApplicationDevice;
import ru.mtuci.rbpopr.model.ApplicationUser;
import ru.mtuci.rbpopr.repository.DeviceRepository;

import java.util.Optional;

@Service
public class DeviceServiceImpl {
    private final DeviceRepository deviceRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Optional<ApplicationDevice> getDeviceByIdAndUser(ApplicationUser user, Long id) {
        return deviceRepository.findByIdAndUser(id, user);
    }

    public Optional<ApplicationDevice> getDeviceByInfo(ApplicationUser user, String mac_address, String name) {
        return deviceRepository.findByUserAndMacAddressAndName(user, mac_address, name);
    }

    public void deleteLastDevice(ApplicationUser user) {
        Optional<ApplicationDevice> lastDevice = deviceRepository.findTopByUserOrderByIdDesc(user);
        lastDevice.ifPresent(deviceRepository::delete);
    }

    public ApplicationDevice registerOrUpdateDevice(String mac, String name, ApplicationUser user, Long deviceId){
        Optional<ApplicationDevice> device = getDeviceByIdAndUser(user, deviceId);
        ApplicationDevice newDevice = new ApplicationDevice();
        if (device.isPresent()) {
            newDevice = device.get();
        }

        newDevice.setName(name);
        newDevice.setMacAddress(mac);
        newDevice.setUser(user);

        deviceRepository.save(newDevice);
        return newDevice;
    }
}
