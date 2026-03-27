package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.Category;

public interface CategoryService {
    List<Category> getAllCategories();
    Category getCategoryById(Long id);
    Category saveCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
}
