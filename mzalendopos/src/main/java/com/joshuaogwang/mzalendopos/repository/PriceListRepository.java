package com.joshuaogwang.mzalendopos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.joshuaogwang.mzalendopos.entity.PriceList;
import com.joshuaogwang.mzalendopos.entity.PriceListType;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    List<PriceList> findByActiveTrue();

    Optional<PriceList> findByType(PriceListType type);
}
