# Dashboard administrativo — requisitos

Painel para o responsável pelo estabelecimento acompanhar vendas, pedidos e produtos a partir dos dados já persistidos no Pediu Patrão.

## Requisitos funcionais

### RF01 — Indicadores gerais de vendas

O painel apresenta, para o período consultado:

- faturamento dos pedidos com status `FINALIZADO`;
- ticket médio (faturamento dividido pela quantidade de pedidos finalizados);
- total de pedidos;
- quantidade de itens vendidos nos pedidos finalizados;
- quantidades de pedidos finalizados, em aberto e cancelados.

Pedido em aberto é todo pedido que não está `FINALIZADO` nem `CANCELADO`.

### RF02 — Análise de vendas (gráficos)

O painel exibe um gráfico de barras com o faturamento dos pedidos finalizados, agrupado por dia ou por mês, usando a data de entrada do pedido. Quando o período é informado, dias ou meses sem venda aparecem com valor zero.

### RF03 — Situação dos pedidos

O painel exibe um gráfico de rosca com a quantidade de pedidos em cada status do fluxo já existente no sistema: `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO` e `CANCELADO`. A API também devolve o percentual de cada status.

### RF04 — Produtos mais vendidos

O painel lista o ranking dos produtos dos pedidos finalizados, com tipo, quantidade e faturamento. O faturamento do item é a participação dele no `valorTotal` do pedido, para respeitar desconto já aplicado. O responsável escolhe o tamanho do ranking (1 a 20).

### RF05 — Filtros de consulta

Os quatro recursos aceitam `dataInicio` e `dataFim` (`yyyy-MM-dd`). Além disso:

- vendas aceitam `agrupamento` (`DIA` ou `MES`);
- o ranking aceita `tipo` (Pizza, Esfiha, Lanche, Bebida ou todos) e `limite`.

A tela abre com os últimos 30 dias preenchidos. Período invertido e limite fora da faixa são recusados antes da consulta e também na API.

### RF06 — Integração com a API

A página `/dashboard` não calcula indicadores no servidor de tela. O navegador consulta:

- `GET /api/dashboard/indicadores`
- `GET /api/dashboard/vendas`
- `GET /api/dashboard/pedidos/situacao`
- `GET /api/dashboard/produtos/mais-vendidos`

O acesso é restrito aos perfis `ADMIN` e `GERENTE`, tanto na página quanto nos endpoints.

### RF07 — Atualização e tratamento de dados

O botão **Atualizar** refaz as quatro consultas com os filtros atuais e registra o horário da última atualização. A tela trata:

- falha de rede ou resposta de erro da API, com mensagem visível;
- HTTP 403, informando falta de permissão;
- período sem pedidos ou sem vendas, com texto de estado vazio;
- parâmetros inválidos, com HTTP 400 e mensagem em texto simples.

## Requisitos não funcionais

### RNF01 — Segurança

Os endpoints exigem sessão autenticada. `ATENDENTE` não consulta o dashboard. A autorização está no filtro HTTP e repetida com `@PreAuthorize` no serviço.

### RNF02 — Consistência com o domínio

O dashboard só lê pedidos e produtos. Não altera status, desconto, cancelamento nem auditoria. Cancelados não entram em faturamento, gráfico de vendas nem ranking.

### RNF03 — Formato

As respostas de sucesso são JSON. Datas de filtro usam ISO `yyyy-MM-dd`. Valores monetários são números com duas casas decimais, sem símbolo.

### RNF04 — Desempenho

A agregação ocorre em memória sobre os pedidos do estabelecimento. O volume esperado de uma lanchonete cabe nesse modelo e evita uma coleção paralela que poderia divergir do pedido real. Se o histórico crescer muito, a mesma API pode passar a usar agregação no MongoDB sem mudar o contrato do dashboard.

### RNF05 — Manutenibilidade

Cada recurso do painel tem um endpoint próprio. A regra de período, faturamento e ranking fica em `DashboardService`, coberta por testes unitários.

### RNF06 — Usabilidade

O painel usa o layout, o menu e o Bootstrap já adotados no Pediu Patrão. Valores são exibidos em real (`pt-BR`).

## Decisões de projeto

| Decisão | Motivo |
|---|---|
| Faturamento só de `FINALIZADO` | Venda realizada, separada do que ainda está na operação |
| Data de entrada como eixo do gráfico | Todo pedido tem essa data; a data de saída só existe depois da entrega ou retirada |
| Ranking com valor proporcional ao total | O desconto do pedido continua refletido no produto |
| Leitura direta da coleção `pedidos` | Uma única fonte de verdade com o restante do sistema |
| Perfis ADMIN e GERENTE | O painel apoia a gestão; o atendente continua no registro de pedidos |
