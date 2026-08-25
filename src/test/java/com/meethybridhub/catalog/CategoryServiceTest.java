package com.meethybridhub.catalog;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository repository;

    @Test
    void createNormalizesNameAndUsesSameStoreParent() {
        Category parent = category(10L, 7L, "Shoes");
        when(repository.findByStoreIdAndName(7L, "Men")).thenReturn(Optional.empty());
        when(repository.findByIdAndStoreId(10L, 7L)).thenReturn(Optional.of(parent));
        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = new CategoryService(repository).create(7L, " Men ", "  menswear ", 10L);

        assertThat(result.getStoreId()).isEqualTo(7L);
        assertThat(result.getName()).isEqualTo("Men");
        assertThat(result.getDescription()).isEqualTo("menswear");
        assertThat(result.getParentCategory()).isSameAs(parent);
    }

    @Test
    void createRejectsDuplicateAndForeignParent() {
        when(repository.findByStoreIdAndName(7L, "Shoes")).thenReturn(Optional.of(category(1L, 7L, "Shoes")));
        assertThatThrownBy(() -> new CategoryService(repository).create(7L, "Shoes", null, null))
                .isInstanceOf(BadRequestException.class);

        when(repository.findByStoreIdAndName(7L, "Kids")).thenReturn(Optional.empty());
        when(repository.findByIdAndStoreId(99L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new CategoryService(repository).create(7L, "Kids", null, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listUpdateAndDeleteAreTenantScoped() {
        Category existing = category(4L, 7L, "Old");
        when(repository.findAllByStoreId(7L)).thenReturn(List.of(existing));
        assertThat(new CategoryService(repository).list(7L)).containsExactly(existing);

        when(repository.findByIdAndStoreId(4L, 7L)).thenReturn(Optional.of(existing));
        when(repository.findByStoreIdAndName(7L, "New")).thenReturn(Optional.empty());
        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Category updated = new CategoryService(repository).update(7L, 4L, "New", "desc", null);
        assertThat(updated.getName()).isEqualTo("New");
        new CategoryService(repository).delete(7L, 4L);
        verify(repository).delete(existing);
    }

    @Test
    void updateRejectsSelfParentAndDuplicate() {
        Category existing = category(4L, 7L, "Old");
        when(repository.findByIdAndStoreId(4L, 7L)).thenReturn(Optional.of(existing));
        when(repository.findByStoreIdAndName(7L, "New")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new CategoryService(repository).update(7L, 4L, "New", null, 4L))
                .isInstanceOf(BadRequestException.class);

        Category duplicate = category(5L, 7L, "New");
        when(repository.findByStoreIdAndName(7L, "New")).thenReturn(Optional.of(duplicate));
        assertThatThrownBy(() -> new CategoryService(repository).update(7L, 4L, "New", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    private Category category(Long id, Long storeId, String name) {
        Category category = new Category(storeId, name, null, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
