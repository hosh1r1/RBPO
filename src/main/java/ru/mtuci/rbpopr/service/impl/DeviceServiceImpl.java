package ru.mtuci.rbpopr.service.impl;

import org.springframework.stereotype.Service;
import ru.mtuci.rbpopr.model.ApplicationDevice;
import ru.mtuci.rbpopr.model.ApplicationUser;
import ru.mtuci.rbpopr.repository.DeviceRepository;

import java.util.Optional;

//TODO: 1. Оптимизировать метод registerOrUpdateDevice

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

    public ApplicationDevice registerOrUpdateDevice(String mac, String name, ApplicationUser user, Long deviceId) {
        ApplicationDevice device = getDeviceByIdAndUser(user, deviceId)
                .orElse(new ApplicationDevice());

        device.setName(name);
        device.setMacAddress(mac);
        device.setUser(user);

        return deviceRepository.save(device);
    }

}
