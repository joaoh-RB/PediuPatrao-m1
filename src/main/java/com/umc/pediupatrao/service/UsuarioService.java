package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> PERFIS = Set.of("ADMIN", "GERENTE", "ATENDENTE");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void salvarUsuario(Usuario usuario) {
        validarPerfil(usuario.getRole());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        usuarioRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        usuarioRepository.findById(id).ifPresent(usuarioExistente -> {
            validarPerfil(usuarioAtualizado.getRole());
            usuarioExistente.setUsername(usuarioAtualizado.getUsername());
            usuarioExistente.setRole(usuarioAtualizado.getRole());
            if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
                usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
            }
            usuarioRepository.save(usuarioExistente);
        });
    }

    private void validarPerfil(String role) {
        if (role == null || !PERFIS.contains(role)) {
            throw new IllegalArgumentException("Perfil inválido.");
        }
    }
}
