package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        http
                .csrf(csrf -> csrf.csrfTokenRequestHandler(requestHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/auditoria/**", "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/clientes/novo", "/clientes/editar/**", "/clientes/salvar").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/clientes", "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/clientes", "/clientes/**", "/api/clientes", "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/novo", "/pedidos/salvar").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("ATENDENTE")
                        .requestMatchers("/pedidos/*/cancelar", "/pedidos/*/desconto").hasRole("GERENTE")
                        .requestMatchers("/api/pedidos/*/cancelar", "/api/pedidos/*/desconto").hasRole("GERENTE")
                        .requestMatchers("/pedidos/*/status").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/api/pedidos", "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/");
        };
    }
}
