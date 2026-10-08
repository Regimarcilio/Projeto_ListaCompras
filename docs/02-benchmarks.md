# 02 — Benchmarks — ListaCompras (pesquisa 2026-10-07)

> Método planmaster-benchmark: 3 websearch + 2 webfetch. Só citado o retornado.

## 1. Tabela comparativa

| Solução | Features relevantes | Licença | Atividade | Prós | Contras | Custo |
|---|---|---|---|---|---|---|
| **GroceryGenius (DanielRendox)** — github.com/DanielRendox/GroceryGenius | Compose single-activity, Room, DataStore, Coroutines/Flow, MVI + Clean, offline-first, 130 produtos seed, Material You | GPL-3.0 | 145 stars, 18 forks, 452 commits, branch develop | Arquitetura ideal para copiar (Room=fonte verdade, Flow reativo); seed catálogo; Material3 | GPL copyleft (se copiar código, abrir fonte); sem share/dashboard por tipo pronto; sync via GitHub REST | Grátis |
| **thespation/lista-compras-android** | 100% offline, histórico preços, análise gastos ano/mês/mercado, duplicar lista, basear em anterior, backup JSON, WebView+React+Java bridge | MIT | 2026-02, 0 stars (novo) | Requisitos quase idênticos ao pedido (duplicar, basear, histórico, backup); MIT permissiva | WebView (não nativo Compose); sem Room tipado; performance inferior | Grátis |
| **Erick-Lim-Souza/Lista_de_Compras (PWA)** | Offline-first PWA, tipos (Supermercado/Feira/Açougue/Farmácia...), atacado/varejo, dashboard 13 gráficos, backup JSON merge, zero build | N/A (Vercel) | Ativo 2026-04 | Melhor referência de BI por tipo e ciclos consumo; tipos por lista; export TXT/PDF/CSV | Web (não Play Store nativa); sem Room; sem Intent nativo | Grátis |
| **paleobyleo/ShoppingList + wbpxre150/android-shoppinglist** | Compose+Room+MVVM+Flow, multi-listas, import/export `.shoppinglist` (JSON) via FileProvider, share e-mail/msg | MIT-like | Ativos 2025-26 | Prova padrão export JSON + FileProvider + Share Intent (exatamente nosso RF-04/05); queries SUM por tipo | Sem dashboard maduro; sem seed amplo | Grátis |
| **ListaSimples (listasimples.com, BR)** | Listas por categoria, orçamento realtime, ofertas por CEP, offline PWA | Comercial freemium | Produto ativo BR 2026 | Valida dor BR + orçamento realtime no topo (nosso header acumulado) | Fechado, com conta/cloud; fora do escopo offline puro | Freemium |
| **Docs oficiais Room + Share (developer.android.com)** | Room 3.0.3 KSP-only + DataStore boundary; `ACTION_SEND` + `createChooser()` + FileProvider `EXTRA_STREAM` | Docs Google | Atualizado set/2026 | Fonte normativa para ADR-001/002: Room p/ tabelas, DataStore p/ prefs; Intent sem permissão perigosa | — | — |

Webfetch: `github.com/DanielRendox/GroceryGenius` (OK, stack confirmada) + `developer.android.com/training/data-storage/room` (timeout, mas conteúdo já coberto via websearch snippets Room 3.0.3).

## 2. Decisão técnica justificada

- **Ideal: Kotlin nativo (Compose + Room + DataStore + Hilt)** — converge com GroceryGenius + ShoppingList + docs Google; atende offline, Flow reativo p/ total acumulado, FileProvider p/ JSON, Canvas p/ BI sem lib pesada. minSdk 26.
- **Alternativa: PWA/React (thespation/Erick)** — mais rápido se time só web, mas perde Play Store nativa, performance 500 itens e Intent tipado. Guardar como fallback WebView.
- **Evitar: Flutter/KMP agora** — curva nova, sem ganho para MVP só-Android; reavaliar se pedir iOS.
- **Share: Intent `ACTION_SEND` texto + `application/json`** — evita custo BSP WhatsApp (como já mapeado no Projeto_Doctor `WHATSAPP-BSP.md`); BSP só em roadmap PJ. Sem `SEND_SMS` (usar `ACTION_SENDTO smsto:`).

## 3. Seed / reaproveitamento
- Seed inicial 8 itens (banana, alface, patinho, frango, arroz, feijão, água sanitária, detergente) inspirado nos 130 do Genius + histórico do BR.
- Padrão `.shoppinglist` JSON do wbpxre150 como base do nosso schema v2 (com `exportedAt`, `dataCompra` ISO-8601).
- Dashboard: filtros 7D/30D + por tipo do PWA Erick como roadmap v1.1 (MVP só por lista + global).
