package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.CampoAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String operacao, String entidade, String entidadeId,
            List<CampoAuditoria> campos, String justificativa) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(authentication.getName());
        auditoria.setPerfil(perfilAtual(authentication));
        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setCampos(campos);
        auditoria.setJustificativa(justificativa);
        auditoria.setDataHora(LocalDateTime.now());
        auditoriaRepository.save(auditoria);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<Auditoria> listar() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }

    private String perfilAtual(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .findFirst()
                .orElse("");
    }
}
