package com.meethybridhub.catalog;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category create(Long storeId, String name, String description, Long parentId) {
        String normalizedName = name.trim();
        if (categoryRepository.findByStoreIdAndName(storeId, normalizedName).isPresent()) {
            throw new BadRequestException("Category already exists: " + normalizedName);
        }
        Category parent = parentId == null ? null : categoryRepository.findByIdAndStoreId(parentId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + parentId));
        return categoryRepository.save(new Category(storeId, normalizedName, blankToNull(description), parent));
    }

    @Transactional(readOnly = true)
    public List<Category> list(Long storeId) {
        return categoryRepository.findAllByStoreId(storeId);
    }

    public Category update(Long storeId, Long categoryId, String name, String description, Long parentId) {
        Category category = categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        String normalizedName = name.trim();
        categoryRepository.findByStoreIdAndName(storeId, normalizedName)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> { throw new BadRequestException("Category already exists: " + normalizedName); });
        if (parentId != null && parentId.equals(categoryId)) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        Category parent = parentId == null ? null : categoryRepository.findByIdAndStoreId(parentId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + parentId));
        category.setName(normalizedName);
        category.setDescription(blankToNull(description));
        category.setParentCategory(parent);
        return categoryRepository.save(category);
    }

    public void delete(Long storeId, Long categoryId) {
        Category category = categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        categoryRepository.delete(category);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
