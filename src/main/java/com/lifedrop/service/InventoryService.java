package com.lifedrop.service;

import com.lifedrop.dto.request.InventoryUpdateDto;
import com.lifedrop.dto.response.InventoryResponse;
import com.lifedrop.entity.BloodInventory;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.repository.BloodInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final BloodInventoryRepository inventoryRepository;

    public InventoryService(BloodInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return Arrays.stream(BloodGroup.values())
                .map(this::getOrCreateInventory)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByBloodGroup(BloodGroup bloodGroup) {
        BloodInventory inventory = getOrCreateInventory(bloodGroup);
        return mapToResponse(inventory);
    }

    @Transactional
    public InventoryResponse updateInventory(BloodGroup bloodGroup, InventoryUpdateDto dto) {
        BloodInventory inventory = getOrCreateInventory(bloodGroup);

        if (dto.getAvailableUnits() != null) {
            inventory.setAvailableUnits(dto.getAvailableUnits());
        }
        if (dto.getReservedUnits() != null) {
            inventory.setReservedUnits(dto.getReservedUnits());
        }
        if (dto.getMinimumRequiredUnits() != null) {
            inventory.setMinimumRequiredUnits(dto.getMinimumRequiredUnits());
        }

        BloodInventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    @Transactional
    public void addStock(BloodGroup bloodGroup, int units) {
        if (units <= 0) return;
        BloodInventory inventory = getOrCreateInventory(bloodGroup);
        inventory.setAvailableUnits(inventory.getAvailableUnits() + units);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void deductStock(BloodGroup bloodGroup, int units) {
        if (units <= 0) return;
        BloodInventory inventory = getOrCreateInventory(bloodGroup);
        if (inventory.getAvailableUnits() < units) {
            throw new BadRequestException("Insufficient blood units available for blood group " + bloodGroup.getDisplayValue());
        }
        inventory.setAvailableUnits(inventory.getAvailableUnits() - units);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public BloodInventory getOrCreateInventory(BloodGroup bloodGroup) {
        return inventoryRepository.findByBloodGroup(bloodGroup)
                .orElseGet(() -> {
                    BloodInventory newInv = new BloodInventory(bloodGroup, 0, 15);
                    return inventoryRepository.save(newInv);
                });
    }

    public InventoryResponse mapToResponse(BloodInventory inventory) {
        InventoryResponse response = new InventoryResponse();
        response.setId(inventory.getId());
        response.setBloodGroup(inventory.getBloodGroup());
        response.setAvailableUnits(inventory.getAvailableUnits());
        response.setReservedUnits(inventory.getReservedUnits());
        response.setMinimumRequiredUnits(inventory.getMinimumRequiredUnits());
        response.setStatus(inventory.calculateStatus());
        response.setDemandBadge(inventory.calculateDemandBadge());
        response.setLastUpdated(inventory.getLastUpdated());
        return response;
    }
}
