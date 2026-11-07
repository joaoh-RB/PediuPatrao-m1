# PediuPatrão — Atividade Prática M1 

## Como executar

1. Java 21 e Maven Wrapper (`mvnw.cmd` no Windows).
2. Na pasta do projeto:

```
mvnw.cmd spring-boot:run
```

3. Abrir `http://localhost:8080`.

### Usuários iniciais (senha `teste123`)

| Usuário    | Perfil    |
|------------|-----------|
| admin      | ADMIN     |
| gerente    | GERENTE   |
| atendente  | ATENDENTE |

## Requisitos implementados

- **RF01** — Perfis ADMIN, GERENTE e ATENDENTE. Autorização no Spring Security (rotas e `@PreAuthorize` nos serviços). Cadastro de usuários e perfis apenas pelo ADMIN. CSRF habilitado nos formulários.
- **RF02** — Pedido com itens, quantidades, preços do cadastro de produtos, desconto, total calculado no servidor, responsável pelo registro e data/hora de entrada.
- **RF03** — Fluxo RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA ou RETIRADO → FINALIZADO. Saída registra data/hora e responsável. Transições inválidas bloqueadas. CANCELADO não permite nova alteração de status.
- **RF04** — Cancelamento e desconto exclusivos do GERENTE (ADMIN não consegue). Cancelamento com justificativa, só antes da saída. Desconto percentual até 20%, só antes da saída.
- **RF05** — Auditoria persistida no MongoDB (inclusão/status/desconto/cancelamento de pedidos e alteração de clientes). Consulta só para ADMIN e GERENTE. Sem endpoints de alteração/exclusão de auditoria. Senhas não são gravadas nem devolvidas na API de usuários.

## Dashboard administrativo

Página `/dashboard` (perfis `ADMIN` e `GERENTE`), alimentada por `GET /api/dashboard/**`.

Documentação da atividade:

- [Requisitos](docs/dashboard/requisitos.md)
- [Endpoints](docs/dashboard/api.md)
- [Testes](docs/dashboard/testes.md)
- [Roteiro do vídeo](docs/dashboard/roteiro-apresentacao.md)

Na primeira execução, se não houver pedidos, o sistema grava um histórico de demonstração para o painel.
