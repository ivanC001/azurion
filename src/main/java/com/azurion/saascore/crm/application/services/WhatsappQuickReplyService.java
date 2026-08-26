package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.SaveWhatsappQuickReplyRequest;
import com.azurion.saascore.crm.application.dto.WhatsappQuickReplyResponse;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappQuickReply;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappQuickReplyRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsappQuickReplyService {

    private final CrmWhatsappQuickReplyRepository repository;

    @Transactional(readOnly = true)
    public List<WhatsappQuickReplyResponse> listMine() {
        return repository.findAllByUsuarioIdOrderBySlotAsc(currentUser()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WhatsappQuickReplyResponse create(SaveWhatsappQuickReplyRequest request) {
        String user = currentUser();
        List<CrmWhatsappQuickReply> existing = repository.findAllByUsuarioIdOrderBySlotAsc(user);
        Set<Integer> occupied = existing.stream().map(CrmWhatsappQuickReply::getSlot).collect(java.util.stream.Collectors.toSet());
        int slot = java.util.stream.IntStream.rangeClosed(1, 3)
                .filter(candidate -> !occupied.contains(candidate))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_RESPUESTAS_RAPIDAS_LIMITE",
                        "Cada asesor puede guardar hasta 3 respuestas rapidas"
                ));
        CrmWhatsappQuickReply reply = new CrmWhatsappQuickReply();
        reply.setUsuarioId(user);
        reply.setSlot(slot);
        apply(reply, request);
        return toResponse(repository.save(reply));
    }

    @Transactional
    public WhatsappQuickReplyResponse update(Long id, SaveWhatsappQuickReplyRequest request) {
        CrmWhatsappQuickReply reply = requireOwned(id);
        apply(reply, request);
        return toResponse(repository.save(reply));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireOwned(id));
    }

    private CrmWhatsappQuickReply requireOwned(Long id) {
        return repository.findByIdAndUsuarioId(id, currentUser())
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_RESPUESTA_RAPIDA_NO_ENCONTRADA",
                        "La respuesta rapida no existe o pertenece a otro asesor"
                ));
    }

    private void apply(CrmWhatsappQuickReply reply, SaveWhatsappQuickReplyRequest request) {
        reply.setTitulo(request.titulo().trim());
        reply.setMensaje(request.mensaje().trim());
    }

    private WhatsappQuickReplyResponse toResponse(CrmWhatsappQuickReply reply) {
        return new WhatsappQuickReplyResponse(
                reply.getId(),
                reply.getSlot(),
                reply.getTitulo(),
                reply.getMensaje(),
                reply.getCreatedAt(),
                reply.getUpdatedAt()
        );
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("CRM_USUARIO_REQUERIDO", "Inicia sesion para administrar tus respuestas rapidas");
        }
        return authentication.getName();
    }
}
