package com.joshuaogwang.mzalendopos.service.serviceImpl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joshuaogwang.mzalendopos.dto.PriceListEntryRequest;
import com.joshuaogwang.mzalendopos.dto.PriceListRequest;
import com.joshuaogwang.mzalendopos.entity.PriceList;
import com.joshuaogwang.mzalendopos.entity.PriceListEntry;
import com.joshuaogwang.mzalendopos.entity.Product;
import com.joshuaogwang.mzalendopos.entity.ProductVariant;
import com.joshuaogwang.mzalendopos.repository.PriceListEntryRepository;
import com.joshuaogwang.mzalendopos.repository.PriceListRepository;
import com.joshuaogwang.mzalendopos.repository.ProductRepository;
import com.joshuaogwang.mzalendopos.repository.ProductVariantRepository;
import com.joshuaogwang.mzalendopos.service.PriceListService;

@Service
@Transactional
public class PriceListServiceImpl implements PriceListService {

    @Autowired
    private PriceListRepository priceListRepository;

    @Autowired
    private PriceListEntryRepository entryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Override
    public PriceList createPriceList(PriceListRequest request) {
        PriceList priceList = new PriceList();
        priceList.setName(request.getName());
        priceList.setType(request.getType());
        priceList.setActive(true);
        return priceListRepository.save(priceList);
    }

    @Override
    public PriceListEntry addEntry(Long priceListId, PriceListEntryRequest request) {
        PriceList priceList = priceListRepository.findById(priceListId)
                .orElseThrow(() -> new NoSuchElementException("PriceList not found with id: " + priceListId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + request.getProductId()));

        PriceListEntry entry = new PriceListEntry();
        entry.setPriceList(priceList);
        entry.setProduct(product);
        entry.setPrice(request.getPrice());

        if (request.getVariantId() != null) {
            ProductVariant variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NoSuchElementException("ProductVariant not found with id: " + request.getVariantId()));
            entry.setVariant(variant);
        }

        return entryRepository.save(entry);
    }

    @Override
    public void removeEntry(Long entryId) {
        if (!entryRepository.existsById(entryId)) {
            throw new NoSuchElementException("PriceListEntry not found with id: " + entryId);
        }
        entryRepository.deleteById(entryId);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceList getPriceListById(Long id) {
        return priceListRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PriceList not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceList> getAllPriceLists() {
        return priceListRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> getPriceForProduct(Long priceListId, Long productId, Long variantId) {
        // Try variant-specific entry first
        if (variantId != null) {
            Optional<PriceListEntry> variantEntry =
                    entryRepository.findByPriceListIdAndProductIdAndVariantId(priceListId, productId, variantId);
            if (variantEntry.isPresent()) {
                return Optional.of(variantEntry.get().getPrice());
            }
        }

        // Fall back to product-level entry
        Optional<PriceListEntry> productEntry =
                entryRepository.findByPriceListIdAndProductId(priceListId, productId);
        if (productEntry.isPresent()) {
            return Optional.of(productEntry.get().getPrice());
        }

        return Optional.empty();
    }
}
