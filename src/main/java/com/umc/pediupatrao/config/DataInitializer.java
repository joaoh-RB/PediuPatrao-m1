package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository, ProdutoRepository produtoRepository) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            criarUsuario(usuarioRepository, encoder, "admin", "ADMIN");
            criarUsuario(usuarioRepository, encoder, "gerente", "GERENTE");
            criarUsuario(usuarioRepository, encoder, "atendente", "ATENDENTE");

            if (produtoRepository.count() == 0) {
                Produto pizza = new Produto();
                pizza.setNome("Pizza Mussarela");
                pizza.setTipo("Pizza");
                pizza.setPreco(45.00);
                pizza.setAtivo(true);
                produtoRepository.save(pizza);

                Produto calabresa = new Produto();
                calabresa.setNome("Pizza Calabresa");
                calabresa.setTipo("Pizza");
                calabresa.setPreco(48.00);
                calabresa.setAtivo(true);
                produtoRepository.save(calabresa);
            }
        };
    }

    private void criarUsuario(UsuarioRepository repo, BCryptPasswordEncoder encoder, String username, String role) {
        repo.findByUsername(username).ifPresentOrElse(usuario -> {
            if (usuario.getRole() == null || usuario.getRole().isBlank() || "USER".equals(usuario.getRole())) {
                usuario.setRole(role);
                repo.save(usuario);
            }
        }, () -> {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(encoder.encode("teste123"));
            usuario.setRole(role);
            repo.save(usuario);
        });
    }
}
