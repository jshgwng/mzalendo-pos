package com.joshuaogwang.mzalendopos.service;

import java.util.List;
import java.util.Optional;

import com.joshuaogwang.mzalendopos.dto.PriceListEntryRequest;
import com.joshuaogwang.mzalendopos.dto.PriceListRequest;
import com.joshuaogwang.mzalendopos.entity.PriceList;
import com.joshuaogwang.mzalendopos.entity.PriceListEntry;

public interface PriceListService {

    PriceList createPriceList(PriceListRequest request);

    PriceListEntry addEntry(Long priceListId, PriceListEntryRequest request);

    void removeEntry(Long entryId);

    PriceList getPriceListById(Long id);

    List<PriceList> getAllPriceLists();

    Optional<Double> getPriceForProduct(Long priceListId, Long productId, Long variantId);
}
