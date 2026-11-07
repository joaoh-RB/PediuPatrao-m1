# API do dashboard administrativo

Base: `http://localhost:8080`

Autenticação: sessão do formulário de login do Pediu Patrão (cookie). Perfis permitidos: `ADMIN` e `GERENTE`.

Usuários locais de demonstração (senha `teste123`): `admin`, `gerente`.

Todas as datas são `yyyy-MM-dd`. Sem `dataInicio` e `dataFim`, a consulta considera todo o histórico que possui data de entrada.

## GET /api/dashboard/indicadores

Indicadores gerais de vendas (RF01).

### Parâmetros

| Nome | Obrigatório | Descrição |
|---|---|---|
| dataInicio | não | Início do período, inclusive |
| dataFim | não | Fim do período, inclusive |

### Exemplo de requisição

```http
GET /api/dashboard/indicadores?dataInicio=2026-10-08&dataFim=2026-11-07 HTTP/1.1
Accept: application/json
```

### Exemplo de resposta — 200

```json
{
  "totalPedidos": 13,
  "pedidosFinalizados": 7,
  "pedidosCancelados": 1,
  "pedidosEmAberto": 5,
  "faturamento": 524.55,
  "ticketMedio": 74.94,
  "quantidadeItensVendidos": 32,
  "dataInicio": "2026-10-08",
  "dataFim": "2026-11-07"
}
```

`faturamento` e `ticketMedio` usam apenas pedidos `FINALIZADO`. `quantidadeItensVendidos` também. Os números do exemplo dependem da base consultada.

## GET /api/dashboard/vendas

Série para o gráfico de vendas (RF02).

### Parâmetros

| Nome | Obrigatório | Descrição |
|---|---|---|
| dataInicio | não | Início do período |
| dataFim | não | Fim do período |
| agrupamento | não | `DIA` (padrão) ou `MES` |

### Exemplo de requisição

```http
GET /api/dashboard/vendas?dataInicio=2026-11-01&dataFim=2026-11-03&agrupamento=DIA HTTP/1.1
Accept: application/json
```

### Exemplo de resposta — 200

```json
{
  "agrupamento": "DIA",
  "dataInicio": "2026-11-01",
  "dataFim": "2026-11-03",
  "serie": [
    { "periodo": "2026-11-01", "faturamento": 50.00, "quantidadePedidos": 1 },
    { "periodo": "2026-11-02", "faturamento": 0.00, "quantidadePedidos": 0 },
    { "periodo": "2026-11-03", "faturamento": 30.00, "quantidadePedidos": 1 }
  ]
}
```

## GET /api/dashboard/pedidos/situacao

Distribuição por status (RF03). Sempre devolve os sete status do fluxo, mesmo com quantidade zero.

### Parâmetros

| Nome | Obrigatório | Descrição |
|---|---|---|
| dataInicio | não | Início do período |
| dataFim | não | Fim do período |

### Exemplo de requisição

```http
GET /api/dashboard/pedidos/situacao?dataInicio=2026-11-01&dataFim=2026-11-01 HTTP/1.1
Accept: application/json
```

### Exemplo de resposta — 200

```json
{
  "total": 4,
  "dataInicio": "2026-11-01",
  "dataFim": "2026-11-01",
  "porStatus": [
    { "status": "RECEBIDO", "quantidade": 0, "percentual": 0.00 },
    { "status": "EM_PREPARACAO", "quantidade": 0, "percentual": 0.00 },
    { "status": "PRONTO", "quantidade": 1, "percentual": 25.00 },
    { "status": "SAIU_PARA_ENTREGA", "quantidade": 0, "percentual": 0.00 },
    { "status": "RETIRADO", "quantidade": 0, "percentual": 0.00 },
    { "status": "FINALIZADO", "quantidade": 2, "percentual": 50.00 },
    { "status": "CANCELADO", "quantidade": 1, "percentual": 25.00 }
  ]
}
```

## GET /api/dashboard/produtos/mais-vendidos

Ranking de produtos (RF04).

### Parâmetros

| Nome | Obrigatório | Descrição |
|---|---|---|
| dataInicio | não | Início do período |
| dataFim | não | Fim do período |
| tipo | não | Tipo cadastrado do produto. Vazio considera todos |
| limite | não | Quantidade de posições, de 1 a 20. Padrão: 5 |

### Exemplo de requisição

```http
GET /api/dashboard/produtos/mais-vendidos?dataInicio=2026-11-01&dataFim=2026-11-07&tipo=Pizza&limite=5 HTTP/1.1
Accept: application/json
```

### Exemplo de resposta — 200

```json
{
  "limite": 5,
  "tipo": "Pizza",
  "dataInicio": "2026-11-01",
  "dataFim": "2026-11-07",
  "produtos": [
    {
      "nome": "Pizza Mussarela",
      "tipo": "Pizza",
      "quantidade": 2,
      "faturamento": 72.00
    }
  ]
}
```

O `faturamento` reparte o `valorTotal` do pedido finalizado na proporção do preço de cada item. Pedido cancelado ou ainda em aberto não entra no ranking.

## Erros

Corpo em `text/plain`.

| HTTP | Quando | Exemplo de corpo |
|---|---|---|
| 400 | `dataInicio` posterior a `dataFim` | `A data inicial não pode ser posterior à data final.` |
| 400 | `agrupamento` diferente de `DIA` e `MES` | `Agrupamento inválido. Use DIA ou MES.` |
| 400 | `limite` menor que 1 ou maior que 20 | `O limite do ranking deve estar entre 1 e 20.` |
| 400 | data em formato inválido | `Parâmetro inválido. Datas devem usar o formato yyyy-MM-dd.` |
| 401 | sem sessão | redirecionamento para `/login` |
| 403 | perfil `ATENDENTE` ou outro sem gerência | acesso negado pelo Spring Security |

### Exemplo de erro

```http
GET /api/dashboard/vendas?agrupamento=SEMANA HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
Content-Type: text/plain

Agrupamento inválido. Use DIA ou MES.
```

## Página integrada

`GET /dashboard` entrega a tela. O script `static/js/dashboard.js` chama os quatro endpoints em paralelo a cada filtro ou atualização.
