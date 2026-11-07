(function () {
    const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
    const alerta = document.getElementById("alertaDashboard");
    const form = document.getElementById("formFiltros");
    let graficoVendas = null;
    let graficoSituacao = null;

    const coresStatus = {
        RECEBIDO: "#6c757d",
        EM_PREPARACAO: "#0d6efd",
        PRONTO: "#20c997",
        SAIU_PARA_ENTREGA: "#fd7e14",
        RETIRADO: "#6f42c1",
        FINALIZADO: "#198754",
        CANCELADO: "#dc3545"
    };

    function hojeIso() {
        const data = new Date();
        const mes = String(data.getMonth() + 1).padStart(2, "0");
        const dia = String(data.getDate()).padStart(2, "0");
        return data.getFullYear() + "-" + mes + "-" + dia;
    }

    function diasAtras(quantidade) {
        const data = new Date();
        data.setDate(data.getDate() - quantidade);
        const mes = String(data.getMonth() + 1).padStart(2, "0");
        const dia = String(data.getDate()).padStart(2, "0");
        return data.getFullYear() + "-" + mes + "-" + dia;
    }

    function filtros() {
        const inicio = document.getElementById("dataInicio").value;
        const fim = document.getElementById("dataFim").value;
        const agrupamento = document.getElementById("agrupamento").value;
        const tipo = document.getElementById("tipo").value;
        const limite = document.getElementById("limite").value || "5";
        if (inicio && fim && inicio > fim) {
            throw new Error("A data inicial não pode ser posterior à data final.");
        }
        const limiteNumero = Number(limite);
        if (!Number.isInteger(limiteNumero) || limiteNumero < 1 || limiteNumero > 20) {
            throw new Error("O ranking deve pedir entre 1 e 20 produtos.");
        }
        const params = new URLSearchParams();
        if (inicio) params.set("dataInicio", inicio);
        if (fim) params.set("dataFim", fim);
        return { params, agrupamento, tipo, limite };
    }

    function query(base, params) {
        const texto = params.toString();
        return texto ? base + "?" + texto : base;
    }

    async function buscar(url) {
        const resposta = await fetch(url, { headers: { Accept: "application/json" } });
        const corpo = await resposta.text();
        if (!resposta.ok) {
            if (resposta.status === 403) {
                throw new Error("Seu perfil não pode consultar o dashboard.");
            }
            throw new Error(corpo || "Não foi possível carregar os dados do dashboard.");
        }
        return corpo ? JSON.parse(corpo) : {};
    }

    function mostrarErro(mensagem) {
        alerta.textContent = mensagem;
        alerta.classList.remove("d-none");
    }

    function limparErro() {
        alerta.classList.add("d-none");
        alerta.textContent = "";
    }

    function preencherIndicadores(dados) {
        document.getElementById("kpiFaturamento").textContent = moeda.format(dados.faturamento || 0);
        document.getElementById("kpiTicket").textContent = moeda.format(dados.ticketMedio || 0);
        document.getElementById("kpiPedidos").textContent = String(dados.totalPedidos || 0);
        document.getElementById("kpiItens").textContent =
            (dados.quantidadeItensVendidos || 0) + " itens em pedidos finalizados";
        document.getElementById("kpiSituacao").textContent =
            (dados.pedidosFinalizados || 0) + " finalizados · " +
            (dados.pedidosEmAberto || 0) + " em aberto · " +
            (dados.pedidosCancelados || 0) + " cancelados";
    }

    function desenharVendas(dados) {
        const serie = dados.serie || [];
        const vazio = document.getElementById("vendasVazio");
        const temValor = serie.some(function (ponto) { return ponto.faturamento > 0; });
        vazio.classList.toggle("d-none", temValor);
        if (graficoVendas) {
            graficoVendas.destroy();
        }
        graficoVendas = new Chart(document.getElementById("graficoVendas"), {
            type: "bar",
            data: {
                labels: serie.map(function (ponto) { return ponto.periodo; }),
                datasets: [{
                    label: "Faturamento",
                    data: serie.map(function (ponto) { return ponto.faturamento; }),
                    backgroundColor: "#0d6efd",
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) { return moeda.format(ctx.parsed.y || 0); }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function (valor) { return moeda.format(valor); }
                        }
                    }
                }
            }
        });
    }

    function desenharSituacao(dados) {
        const fatias = (dados.porStatus || []).filter(function (item) { return item.quantidade > 0; });
        const vazio = document.getElementById("situacaoVazia");
        vazio.classList.toggle("d-none", fatias.length > 0);
        if (graficoSituacao) {
            graficoSituacao.destroy();
        }
        graficoSituacao = new Chart(document.getElementById("graficoSituacao"), {
            type: "doughnut",
            data: {
                labels: fatias.map(function (item) { return item.status.replaceAll("_", " "); }),
                datasets: [{
                    data: fatias.map(function (item) { return item.quantidade; }),
                    backgroundColor: fatias.map(function (item) { return coresStatus[item.status] || "#adb5bd"; })
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: "bottom" }
                }
            }
        });
    }

    function preencherRanking(dados) {
        const corpo = document.getElementById("rankingCorpo");
        const produtos = dados.produtos || [];
        if (produtos.length === 0) {
            corpo.innerHTML = '<tr><td colspan="5" class="text-muted ps-3">Nenhum produto vendido neste filtro.</td></tr>';
            return;
        }
        corpo.innerHTML = produtos.map(function (produto, indice) {
            return "<tr>" +
                "<td class=\"ps-3\">" + (indice + 1) + "</td>" +
                "<td>" + escapar(produto.nome) + "</td>" +
                "<td>" + escapar(produto.tipo) + "</td>" +
                "<td class=\"text-end\">" + produto.quantidade + "</td>" +
                "<td class=\"text-end pe-3\">" + moeda.format(produto.faturamento || 0) + "</td>" +
                "</tr>";
        }).join("");
    }

    function escapar(texto) {
        return String(texto == null ? "" : texto)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
    }

    async function carregar() {
        limparErro();
        let consulta;
        try {
            consulta = filtros();
        } catch (erro) {
            mostrarErro(erro.message);
            return;
        }
        const vendasParams = new URLSearchParams(consulta.params);
        vendasParams.set("agrupamento", consulta.agrupamento);
        const rankingParams = new URLSearchParams(consulta.params);
        rankingParams.set("limite", consulta.limite);
        if (consulta.tipo) {
            rankingParams.set("tipo", consulta.tipo);
        }
        try {
            const [indicadores, vendas, situacao, ranking] = await Promise.all([
                buscar(query("/api/dashboard/indicadores", consulta.params)),
                buscar(query("/api/dashboard/vendas", vendasParams)),
                buscar(query("/api/dashboard/pedidos/situacao", consulta.params)),
                buscar(query("/api/dashboard/produtos/mais-vendidos", rankingParams))
            ]);
            preencherIndicadores(indicadores);
            desenharVendas(vendas);
            desenharSituacao(situacao);
            preencherRanking(ranking);
            document.getElementById("atualizadoEm").textContent =
                "Atualizado em " + new Date().toLocaleString("pt-BR");
        } catch (erro) {
            mostrarErro(erro.message || "Falha ao consultar a API do dashboard.");
        }
    }

    document.getElementById("dataInicio").value = diasAtras(30);
    document.getElementById("dataFim").value = hojeIso();
    form.addEventListener("submit", function (evento) {
        evento.preventDefault();
        carregar();
    });
    document.getElementById("btnAtualizar").addEventListener("click", carregar);
    carregar();
})();
