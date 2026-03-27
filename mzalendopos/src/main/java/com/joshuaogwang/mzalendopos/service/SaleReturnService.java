package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.dto.ReturnRequest;
import com.joshuaogwang.mzalendopos.entity.SaleReturn;

public interface SaleReturnService {
    SaleReturn processReturn(ReturnRequest request, String cashierUsername);
    SaleReturn getReturnById(Long id);
    List<SaleReturn> getReturnsByOriginalSale(Long saleId);
}
