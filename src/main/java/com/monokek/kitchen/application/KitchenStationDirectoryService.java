package com.monokek.kitchen.application;

import com.monokek.kitchen.KitchenStationDirectory;
import com.monokek.kitchen.domain.KitchenStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class KitchenStationDirectoryService implements KitchenStationDirectory {

    private final KitchenStationRepository stationRepository;

    KitchenStationDirectoryService(KitchenStationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findPrinterLocation(Long stationId) {
        return stationRepository.findById(stationId).map(station -> station.getType().name().toLowerCase());
    }
}
