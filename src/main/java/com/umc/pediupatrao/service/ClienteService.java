package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAuditoria;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public void excluir(String id) {
        clienteRepository.deleteById(id);
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            cliente.setId(null);
            return clienteRepository.save(cliente);
        }
        Cliente existente = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));
        List<CampoAuditoria> campos = camposAlterados(existente, cliente);
        Cliente salvo = clienteRepository.save(cliente);
        if (!campos.isEmpty()) {
            auditoriaService.registrar("ALTERAÇÃO DE CLIENTE", "Cliente", salvo.getId(), campos, null);
        }
        return salvo;
    }

    private List<CampoAuditoria> camposAlterados(Cliente anterior, Cliente atual) {
        List<CampoAuditoria> campos = new ArrayList<>();
        adicionarSeAlterado(campos, "nome", anterior.getNome(), atual.getNome());
        adicionarSeAlterado(campos, "telefone", anterior.getTelefone(), atual.getTelefone());
        adicionarSeAlterado(campos, "cep", anterior.getCep(), atual.getCep());
        adicionarSeAlterado(campos, "logradouro", anterior.getLogradouro(), atual.getLogradouro());
        adicionarSeAlterado(campos, "numero", anterior.getNumero(), atual.getNumero());
        adicionarSeAlterado(campos, "complemento", anterior.getComplemento(), atual.getComplemento());
        adicionarSeAlterado(campos, "bairro", anterior.getBairro(), atual.getBairro());
        adicionarSeAlterado(campos, "cidade", anterior.getCidade(), atual.getCidade());
        adicionarSeAlterado(campos, "estado", anterior.getEstado(), atual.getEstado());
        adicionarSeAlterado(campos, "endereco", anterior.getEndereco(), atual.getEndereco());
        return campos;
    }

    private void adicionarSeAlterado(List<CampoAuditoria> campos, String campo, String anterior, String atual) {
        String valorAnterior = anterior == null ? "" : anterior;
        String valorNovo = atual == null ? "" : atual;
        if (!valorAnterior.equals(valorNovo)) {
            campos.add(new CampoAuditoria(campo, valorAnterior, valorNovo));
        }
    }
}
