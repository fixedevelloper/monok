package com.monokek.catalog.application;

import com.monokek.common.ApiException;
import com.monokek.catalog.domain.Modifier;
import com.monokek.catalog.domain.ModifierItem;
import com.monokek.catalog.domain.ModifierItemRepository;
import com.monokek.catalog.domain.ModifierRepository;
import com.monokek.catalog.web.dto.CreateModifierItemRequest;
import com.monokek.catalog.web.dto.CreateModifierRequest;
import com.monokek.catalog.web.dto.ModifierDto;
import com.monokek.catalog.web.dto.UpdateModifierRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Application service: port of {@code App\Http\Controllers\Api\Admin\ModifierController}. */
@Service
public class ModifierService {

    private final ModifierRepository modifierRepository;
    private final ModifierItemRepository modifierItemRepository;

    public ModifierService(ModifierRepository modifierRepository, ModifierItemRepository modifierItemRepository) {
        this.modifierRepository = modifierRepository;
        this.modifierItemRepository = modifierItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ModifierDto> list() {
        return modifierRepository.findAll().stream().map(this::toDto).toList();
    }

    /** Real implementation of {@code show} — routed to by Laravel's {@code apiResource('modifiers', ...)}, but never defined in the controller. */
    @Transactional(readOnly = true)
    public ModifierDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public ModifierDto create(CreateModifierRequest request) {
        Modifier modifier = Modifier.create(request.name());
        if (request.items() != null) {
            request.items().forEach(item -> modifier.addItem(item.name(), item.price()));
        }
        return toDto(modifierRepository.save(modifier));
    }

    @Transactional
    public ModifierDto update(Long id, UpdateModifierRequest request) {
        Modifier modifier = findOrThrow(id);
        modifier.rename(request.name());
        return toDto(modifierRepository.save(modifier));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        // modifier_items.modifier_id is ON DELETE CASCADE at the schema level, same as Laravel's comment says.
        modifierRepository.deleteById(id);
    }

    @Transactional
    public ModifierDto addItem(Long modifierId, CreateModifierItemRequest request) {
        Modifier modifier = findOrThrow(modifierId);
        modifier.addItem(request.name(), request.price());
        return toDto(modifierRepository.save(modifier));
    }

    @Transactional
    public void destroyItem(Long itemId) {
        if (!modifierItemRepository.findById(itemId).isPresent()) {
            throw ApiException.notFound("Option introuvable");
        }
        modifierItemRepository.deleteById(itemId);
    }

    private Modifier findOrThrow(Long id) {
        return modifierRepository.findById(id).orElseThrow(() -> ApiException.notFound("Groupe de modificateurs introuvable"));
    }

    private ModifierDto toDto(Modifier modifier) {
        List<ModifierDto.Item> items = modifier.getItems().stream()
                .map(i -> new ModifierDto.Item(i.getId(), i.getName(), i.getPrice()))
                .toList();
        return new ModifierDto(modifier.getId(), modifier.getName(), items);
    }
}
