package com.joshuaogwang.mzalendopos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.PriceListEntry;

@Repository
public interface PriceListEntryRepository extends JpaRepository<PriceListEntry, Long> {

    Optional<PriceListEntry> findByPriceListIdAndProductId(Long priceListId, Long productId);

    Optional<PriceListEntry> findByPriceListIdAndProductIdAndVariantId(Long priceListId, Long productId, Long variantId);

    List<PriceListEntry> findByPriceListId(Long priceListId);
}
