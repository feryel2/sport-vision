package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.entity.Supplier;
import com.sportvision.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Supplier>>> getAllSuppliers() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getAllSuppliers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id)
                .map(sup -> ResponseEntity.ok(ApiResponse.ok(sup)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(@RequestBody Supplier supplier) {
        try {
            Supplier created = supplierService.createSupplier(supplier);
            return ResponseEntity.ok(ApiResponse.ok("Fournisseur créé avec succès", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        try {
            Supplier updated = supplierService.updateSupplier(id, supplier);
            return ResponseEntity.ok(ApiResponse.ok("Fournisseur mis à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            return ResponseEntity.ok(ApiResponse.ok("Fournisseur supprimé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
