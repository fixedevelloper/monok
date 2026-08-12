package com.monokek.floorplan.web;

import com.monokek.floorplan.application.FloorService;
import com.monokek.floorplan.web.dto.CreateFloorRequest;
import com.monokek.floorplan.web.dto.FloorDto;
import com.monokek.floorplan.web.dto.UpdateFloorRequest;
import com.monokek.identity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Port of the floor-related methods of {@code App\Http\Controllers\Api\Pos\TableController}. */
@RestController
@RequestMapping("/api/pos/floors")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    /** Scoped to the caller's own branch — see {@link FloorService#list}. */
    @GetMapping
    public List<FloorDto> index(@AuthenticationPrincipal CurrentUser principal) {
        return floorService.list(principal.branchId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FloorDto store(@Valid @RequestBody CreateFloorRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return floorService.create(request, principal.branchId());
    }

    @PutMapping("/{id}")
    public FloorDto update(@PathVariable Long id, @Valid @RequestBody UpdateFloorRequest request) {
        return floorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        floorService.delete(id);
    }
}
