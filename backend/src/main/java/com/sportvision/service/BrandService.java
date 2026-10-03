package com.sportvision.service;

import com.sportvision.entity.Brand;
import com.sportvision.repository.BrandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Brand> getBrandById(Long id) {
        return brandRepository.findById(id);
    }

    public Brand createBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    public Brand updateBrand(Long id, Brand brandDetails) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marque non trouvée avec l'id : " + id));

        brand.setName(brandDetails.getName());
        brand.setCountry(brandDetails.getCountry());
        brand.setLogoUrl(brandDetails.getLogoUrl());
        brand.setDescription(brandDetails.getDescription());

        return brandRepository.save(brand);
    }

    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);
    }
}
