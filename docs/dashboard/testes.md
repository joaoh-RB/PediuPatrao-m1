# Evidências de teste — dashboard administrativo

Data da execução: 07/11/2026.  
Comando: `mvnw.cmd test -Dtest=DashboardServiceTest`  
Ambiente: Java 21.0.12, Windows 10, JUnit 5 e Mockito (sem gravar no MongoDB).  
Resultado: **5 testes, 0 falhas, 0 erros, 0 ignorados** (suite em 1,981 s).

Classe: `src/test/java/com/umc/pediupatrao/service/DashboardServiceTest.java`.  
Relatório gerado pelo Maven Surefire: `target/surefire-reports/TEST-com.umc.pediupatrao.service.DashboardServiceTest.xml`.

| Teste | O que comprova | Resultado |
|---|---|---|
| `indicadoresConsideramSomentePedidosFinalizadosNoFaturamento` | No período, 3 pedidos; faturamento 100,00 só do finalizado; cancelado e em aberto entram na contagem e ficam fora do ticket | passou |
| `vendasAgrupamPorDiaEPreenchemDiasSemMovimento` | Agrupamento `dia` normalizado para `DIA`; 01/11 = 50, 02/11 = 0, 03/11 = 30 | passou |
| `situacaoInformaQuantidadeEPercentualDeCadaStatus` | 4 pedidos, 7 status, `FINALIZADO` = 2 (50%) e `RECEBIDO` = 0 | passou |
| `rankingIgnoraCanceladosEFiltraPorTipo` | Filtro Pizza ignora bebida e o pedido cancelado; 2 unidades e faturamento proporcional 72,00 | passou |
| `rejeitaPeriodoEAgrupamentoInvalidos` | Data invertida, agrupamento `SEMANA` e limite 0 lançam `IllegalArgumentException` (a API responde 400) | passou |

## Como repetir

Na raiz do projeto:

```
mvnw.cmd test -Dtest=DashboardServiceTest
```

## Conferência manual da API

1. Subir a aplicação com `mvnw.cmd spring-boot:run`.
2. Entrar como `gerente` / `teste123`.
3. Abrir `http://localhost:8080/dashboard`.
4. No navegador, em rede, conferir as quatro chamadas `GET /api/dashboard/...` com status 200.
5. Inverter as datas e conferir a mensagem de período inválido, sem atualizar os gráficos.
6. Entrar como `atendente` e confirmar que `/dashboard` e `/api/dashboard/indicadores` respondem 403.
