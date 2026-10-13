package com.lifedrop.controller;

import com.lifedrop.dto.request.InventoryUpdateDto;
import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.InventoryResponse;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Blood Inventory", description = "Blood stock inventory tracking and dynamic demand level APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Get all blood inventory levels with dynamic demand/urgency badges")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        List<InventoryResponse> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @GetMapping("/{bloodGroup}")
    @Operation(summary = "Get inventory level for a specific blood group")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByBloodGroup(@PathVariable String bloodGroup) {
        BloodGroup bg = BloodGroup.fromString(bloodGroup);
        InventoryResponse inventory = inventoryService.getInventoryByBloodGroup(bg);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @PutMapping("/{bloodGroup}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update inventory units for a blood group (ADMIN only)")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable String bloodGroup,
            @Valid @RequestBody InventoryUpdateDto dto) {

        BloodGroup bg = BloodGroup.fromString(bloodGroup);
        InventoryResponse updated = inventoryService.updateInventory(bg, dto);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", updated));
    }
}
