package com.monokek.kitchen.application;

import com.monokek.common.ApiException;
import com.monokek.kitchen.domain.KitchenStation;
import com.monokek.kitchen.domain.KitchenStationRepository;
import com.monokek.kitchen.domain.KitchenTicketRepository;
import com.monokek.kitchen.domain.StationType;
import com.monokek.kitchen.web.dto.CreateKitchenStationRequest;
import com.monokek.kitchen.web.dto.KitchenStationDto;
import com.monokek.kitchen.web.dto.UpdateKitchenStationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin CRUD for kitchen stations, plus the read used by both the kitchen dashboard
 * (station cards with a pending-ticket badge) and the POS's "Comptoir Bar" screen
 * (finds its station by {@link StationType#BAR} instead of a hardcoded id — see
 * {@code GET /api/kitchen/stations}, still on {@code TicketController} since it's a
 * kitchen/pos-facing read, not an admin one).
 *
 * <p>Stations previously had no create/update/delete endpoint at all — {@code
 * kitchen_stations} rows could only be seeded directly in the database.
 */
@Service
public class KitchenStationService {

    private static final List<String> ACTIVE_TICKET_STATUSES = List.of("pending", "preparing");

    private final KitchenStationRepository stationRepository;
    private final KitchenTicketRepository ticketRepository;

    public KitchenStationService(KitchenStationRepository stationRepository, KitchenTicketRepository ticketRepository) {
        this.stationRepository = stationRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<KitchenStationDto> list(Long branchId) {
        List<KitchenStation> stations = branchId == null ? stationRepository.findAll() : stationRepository.findByBranchId(branchId);
        return stations.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public KitchenStationDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    /** {@code callerBranchId} null means the caller has no assigned branch (owner/super-admin) —
     * only then is {@code request.branchId()} trusted; a branch manager can only ever create a
     * station for their own branch, not pick another one from the dropdown. */
    @Transactional
    public KitchenStationDto create(CreateKitchenStationRequest request, Long callerBranchId) {
        KitchenStation station = new KitchenStation();
        station.setBranchId(callerBranchId != null ? callerBranchId : request.branchId());
        station.setName(request.name());
        station.setType(request.type());
        return toDto(stationRepository.save(station));
    }

    @Transactional
    public KitchenStationDto update(Long id, UpdateKitchenStationRequest request) {
        KitchenStation station = findOrThrow(id);
        if (request.name() != null) station.setName(request.name());
        if (request.type() != null) station.setType(request.type());
        return toDto(stationRepository.save(station));
    }

    /** kitchen_tickets.station_id is ON DELETE CASCADE and categories.kitchen_station_id is ON DELETE SET NULL — see V1__init_schema.sql. */
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        stationRepository.deleteById(id);
    }

    private KitchenStation findOrThrow(Long id) {
        return stationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Station introuvable"));
    }

    private KitchenStationDto toDto(KitchenStation station) {
        long pendingCount = ticketRepository.countByStationIdAndStatusIn(station.getId(), ACTIVE_TICKET_STATUSES);
        return new KitchenStationDto(station.getId(), station.getBranchId(), station.getName(), station.getType(), pendingCount);
    }
}
