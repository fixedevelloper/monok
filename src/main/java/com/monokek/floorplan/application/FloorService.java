package com.monokek.floorplan.application;

import com.monokek.common.ApiException;
import com.monokek.floorplan.domain.Floor;
import com.monokek.floorplan.domain.FloorRepository;
import com.monokek.floorplan.domain.RestaurantTableRepository;
import com.monokek.floorplan.web.dto.CreateFloorRequest;
import com.monokek.floorplan.web.dto.FloorDto;
import com.monokek.floorplan.web.dto.TableDto;
import com.monokek.floorplan.web.dto.UpdateFloorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Application service: port of the floor-related methods of {@code App\Http\Controllers\Api\Pos\TableController}. */
@Service
public class FloorService {

    private final FloorRepository floorRepository;
    private final RestaurantTableRepository tableRepository;

    public FloorService(FloorRepository floorRepository, RestaurantTableRepository tableRepository) {
        this.floorRepository = floorRepository;
        this.tableRepository = tableRepository;
    }

    /**
     * Port of {@code TableController::floors} (and the near-duplicate,
     * inconsistently-shaped {@code index}, which returned raw unwrapped
     * {@code Floor} models — unified into this one response shape).
     *
     * <p>{@code branchId} null means unscoped (an owner/super-admin with no
     * assigned branch, or an admin screen managing every branch) — everyone
     * else only ever sees their own branch's floors, closing off a real leak:
     * without this, any waiter could pick a table belonging to a different
     * branch, and the resulting order's branchId would route its SSE/kitchen
     * notifications to a branch nobody on that order is watching.
     */
    @Transactional(readOnly = true)
    public List<FloorDto> list(Long branchId) {
        List<Floor> floors = branchId == null ? floorRepository.findAll() : floorRepository.findByBranchId(branchId);
        return floors.stream().map(this::toDto).toList();
    }

    /** {@code callerBranchId} null means the caller has no assigned branch (owner/super-admin) —
     * only then is {@code request.branchId()} trusted; a branch manager can only ever create a
     * zone for their own branch, not pick another one from the dropdown. */
    @Transactional
    public FloorDto create(CreateFloorRequest request, Long callerBranchId) {
        if (floorRepository.existsByName(request.name())) {
            throw ApiException.conflict("Une zone porte déjà ce nom.");
        }
        Floor floor = new Floor();
        floor.setBranchId(callerBranchId != null ? callerBranchId : request.branchId());
        floor.setName(request.name());
        return toDto(floorRepository.save(floor));
    }

    @Transactional
    public FloorDto update(Long id, UpdateFloorRequest request) {
        Floor floor = findOrThrow(id);
        if (floorRepository.existsByNameAndIdNot(request.name(), id)) {
            throw ApiException.conflict("Une zone porte déjà ce nom.");
        }
        floor.setName(request.name());
        return toDto(floorRepository.save(floor));
    }

    /** {@code restaurant_tables.floor_id} is {@code ON DELETE CASCADE} (see V1__init_schema.sql) — deleting a
     * zone deletes its tables with it, same as {@code RestaurantTableService#delete} has no extra guard either. */
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        floorRepository.deleteById(id);
    }

    private Floor findOrThrow(Long id) {
        return floorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Zone introuvable"));
    }

    private FloorDto toDto(Floor floor) {
        List<TableDto> tables = tableRepository.findByFloorId(floor.getId()).stream()
                .map(t -> new TableDto(t.getId(), floor.getId(), floor.getName(), t.getName(), t.getSeats(), t.getStatus(), t.isVirtual()))
                .toList();
        return new FloorDto(floor.getId(), floor.getBranchId(), floor.getName(), tables.size(), tables);
    }
}
