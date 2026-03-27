package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.dto.PriceListEntryRequest;
import com.joshuaogwang.mzalendopos.dto.PriceListRequest;
import com.joshuaogwang.mzalendopos.entity.PriceList;
import com.joshuaogwang.mzalendopos.entity.PriceListEntry;
import com.joshuaogwang.mzalendopos.service.PriceListService;

@RestController
@RequestMapping("/api/v1/price-lists")
public class PriceListController {

    @Autowired
    private PriceListService priceListService;

    @PostMapping
    public ResponseEntity<PriceList> createPriceList(@Valid @RequestBody PriceListRequest request) {
        PriceList priceList = priceListService.createPriceList(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(priceList);
    }

    @GetMapping
    public ResponseEntity<List<PriceList>> getAllPriceLists() {
        List<PriceList> priceLists = priceListService.getAllPriceLists();
        return ResponseEntity.ok(priceLists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceList> getById(@PathVariable Long id) {
        PriceList priceList = priceListService.getPriceListById(id);
        return ResponseEntity.ok(priceList);
    }

    @PostMapping("/{id}/entries")
    public ResponseEntity<PriceListEntry> addEntry(
            @PathVariable Long id,
            @Valid @RequestBody PriceListEntryRequest request) {
        PriceListEntry entry = priceListService.addEntry(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Void> removeEntry(@PathVariable Long entryId) {
        priceListService.removeEntry(entryId);
        return ResponseEntity.noContent().build();
    }
}
