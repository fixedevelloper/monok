package com.monokek.floorplan.application;

import com.monokek.common.ApiException;
import com.monokek.floorplan.domain.Floor;
import com.monokek.floorplan.domain.FloorRepository;
import com.monokek.floorplan.domain.RestaurantTable;
import com.monokek.floorplan.domain.RestaurantTableRepository;
import com.monokek.floorplan.web.dto.CreateTableRequest;
import com.monokek.floorplan.web.dto.TableDto;
import com.monokek.floorplan.web.dto.UpdateTableRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service: port of the table CRUD methods of
 * {@code App\Http\Controllers\Api\Pos\TableController} — {@code index},
 * {@code store}, {@code update}, {@code updateStatus}, plus a real
 * {@code show}/{@code destroy} (Laravel's {@code Route::apiResource('tables', ...)}
 * routed to those, but the controller never defined the methods, so they
 * 500 in the source app).
 */
@Service
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final FloorRepository floorRepository;

    public RestaurantTableService(RestaurantTableRepository tableRepository, FloorRepository floorRepository) {
        this.tableRepository = tableRepository;
        this.floorRepository = floorRepository;
    }

    @Transactional(readOnly = true)
    public List<TableDto> list(Long floorId) {
        List<RestaurantTable> tables = floorId == null
                ? tableRepository.findAllByOrderByNameAsc()
                : tableRepository.findByFloorId(floorId);
        return tables.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TableDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public TableDto create(CreateTableRequest request) {
        if (tableRepository.existsByName(request.name())) {
            throw ApiException.conflict("Une table porte déjà ce nom.");
        }
        Floor floor = floorRepository.findById(request.floorId())
                .orElseThrow(() -> ApiException.badRequest("Zone introuvable"));

        RestaurantTable table = new RestaurantTable();
        table.setFloor(floor);
        table.setName(request.name());
        table.setSeats(request.seats() == null ? 4 : request.seats());
        return toDto(tableRepository.save(table));
    }

    @Transactional
    public TableDto update(Long id, UpdateTableRequest request) {
        RestaurantTable table = findOrThrow(id);

        if (request.name() != null) {
            if (tableRepository.existsByNameAndIdNot(request.name(), id)) {
                throw ApiException.conflict("Une table porte déjà ce nom.");
            }
            table.setName(request.name());
        }
        if (request.seats() != null) {
            table.setSeats(request.seats());
        }
        if (request.floorId() != null) {
            Floor floor = floorRepository.findById(request.floorId())
                    .orElseThrow(() -> ApiException.badRequest("Zone introuvable"));
            table.setFloor(floor);
        }

        return toDto(tableRepository.save(table));
    }

    @Transactional
    public TableDto updateStatus(Long id, String status) {
        RestaurantTable table = findOrThrow(id);
        table.changeStatus(status);
        return toDto(tableRepository.save(table));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        tableRepository.deleteById(id);
    }

    private RestaurantTable findOrThrow(Long id) {
        return tableRepository.findById(id).orElseThrow(() -> ApiException.notFound("Table introuvable"));
    }

    private TableDto toDto(RestaurantTable table) {
        return new TableDto(table.getId(), table.getFloor().getId(), table.getFloor().getName(), table.getName(), table.getSeats(), table.getStatus(), table.isVirtual());
    }
}
