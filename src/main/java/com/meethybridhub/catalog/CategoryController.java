package com.meethybridhub.catalog;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.StoreService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
public class CategoryController {

    private final CategoryService categoryService;
    private final StoreService storeService;
    private final UserService userService;

    public CategoryController(CategoryService categoryService, StoreService storeService, UserService userService) {
        this.categoryService = categoryService;
        this.storeService = storeService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CategoryRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        Category category = categoryService.create(TenantContext.requireStoreId(), request.name(), request.description(), request.parentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        return ResponseEntity.ok(categoryService.list(TenantContext.requireStoreId()).stream().map(CategoryResponse::from).toList());
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        return ResponseEntity.ok(CategoryResponse.from(categoryService.update(TenantContext.requireStoreId(), categoryId,
                request.name(), request.description(), request.parentId())));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        categoryService.delete(TenantContext.requireStoreId(), categoryId);
        return ResponseEntity.noContent().build();
    }

    public record CategoryRequest(
            @NotBlank(message = "Category name is required")
            @Size(max = 120, message = "Category name must be at most 120 characters")
            String name,
            @Size(max = 2000, message = "Category description must be at most 2000 characters")
            String description,
            Long parentId) {}

    public record CategoryResponse(Long id, Long storeId, String name, String description, Long parentId) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getStoreId(), category.getName(),
                    category.getDescription(), category.getParentCategory() == null ? null : category.getParentCategory().getId());
        }
    }
}
