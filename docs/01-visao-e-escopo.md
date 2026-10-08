# 01 — Visão e Escopo — App ListaCompras Supermercado (Android Nativo)

> Projeto novo separado: `Projeto_ListaCompras` | Stack: Kotlin nativo | Branch: `develop` | Data: 2026-10-07
> Material de referência do curso: https://integrador-cedup.glide.page/dl/a400f7 — confrontar personas, jornadas e critérios de aceite com esse material antes de codar.

## 1. Problema
Quem faz compra de supermercado com lista em papel/mente estoura o orçamento, esquece itens e não sabe quanto gastou por categoria (hortifruti, carne, mercearia, limpeza...). Apps genéricos exigem conta, internet no mercado e não exportam histórico simples.

## 2. Objetivo
App Android 100% offline-first que permite montar lista ampla, marcar itens no corredor (verde ativo), ver total acumulado no topo em tempo real, salvar em JSON datado, reutilizar catálogo e ver dashboard BI por tipo, com compartilhamento via SMS/WhatsApp por Intent.

## 3. Público-alvo / Personas
- **P1 Dona(o) de casa (principal):** faz compra semanal/mensal, controla orçamento, reusa lista anterior.
- **P2 Jovem dividido:** divide compra com colegas/família, envia lista por WhatsApp.
- **P3 Ofertista:** compara preço por tipo/mercado, quer histórico e dashboard.
- Jornada: Criar lista → adicionar do catálogo ou avulso (nome+preço+tipo) → no mercado ticar itens (verde) vendo total subir → finalizar → JSON salvo + dashboard → reutilizar/enviar próxima vez.

## 4. Escopo IN
- Catálogo reutilizável com nome, preço referência, tipo (HORTIFRUTI, CARNE, MERCEARIA, LIMPEZA, PADARIA, LATICINIOS, BEBIDAS, HIGIENE, CONGELADOS, OUTROS), unidade (un/kg/L...).
- Adição avulsa com valor corrente + tipo.
- Lista ativa: check/uncheck com indicativo verde + header fixo com total acumulado e qtd selecionada.
- Persistência local Room (offline-first), DataStore para filtros/tema.
- Salvar lista finalizada com data completa (ISO-8601 + epoch) e export JSON (`exportedAt`, `dataCompra`, itens com snapshot).
- Import JSON com validação e relatório de erros.
- Dashboard BI: total por tipo (barras), total geral, ticket médio, qtd por tipo.
- Histórico de listas + duplicar/basear em anterior + nova lista zerada.
- Compartilhar via `ACTION_SEND` texto formatado + arquivo JSON via FileProvider (WhatsApp/SMS/e-mail, sem permissão perigosa).
- Busca + filtro por tipo, quantidades, swipe-delete, modo claro/escuro, acessibilidade TalkBack.

## 5. Escopo OUT (Won't / futuro)
- Sem backend/nuvem, sem login, sem sync multi-device (roadmap: WorkManager + backend opcional).
- Sem pagamento, sem leitor barcode (roadmap v2), sem voz, sem geolocalização de ofertas.
- Sem API WhatsApp BSP paga (usar Intent gratuito; BSP só se virar PJ — ver ADR).
- Sem iOS (avaliar KMP só após MVP Android validado).

## 6. Restrições
- minSdk 26 / targetSdk 34, Kotlin 1.9+, Compose BOM + Material3, Room 2.6/3.x + KSP, DataStore, Navigation Compose, Hilt.
- Zero permissão perigosa: sem INTERNET, sem SEND_SMS, sem LOCATION. Só FileProvider.
- LGPD: 100% local, sem tracking, sem PII em log. Export só por ação explícita.
- Performance: seleção de 500 itens <2s sem ANR; Flow reativo.
- Confrontar com material CEDUP Glide antes da Sprint 1 (checklist de telas do curso).

## 7. Critérios de sucesso
- Criar lista com 20 itens em <3 min; ticar item atualiza total em <200ms.
- Reabrir app após kill mantém tudo; JSON exportado abre válido; share abre chooser.
- Dashboard soma por tipo confere com soma manual (tolerância R$0,01).
- Cobertura ≥70% código novo, SAST limpo.

## 8. Stakeholders
PO (usuário), Planmaster (planejamento), Mobilemaster (app), Datamaster (Room/JSON), QAmaster (testes), Ops (Play Store release).
