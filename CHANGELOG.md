# CHANGELOG — ListaCompras (branch DEV)

Formato: `versão — conteúdo — issues`.

## v1.8 — Histórico só com listas fechadas
- Histórico exibe somente `finalizada=1` + vazio amigável; export JSON inalterado. (#10)
- APK: `ListaCompras-v1.8-hist-fechadas-debug.apk`

## v1.7 — Estabelecimento, badge e histórico enxuto
- Pergunta do nome do estabelecimento 1x/lista (Confirmar/X), 🏪 no header + WhatsApp; banco v4→v5 (`estabelecimento`). (#7)
- Fechar lista zera o badge (itens preservados). (#8)
- Histórico: só Ver lista (read-only) + Excluir com confirmação. (#9)
- APK: `ListaCompras-v1.7-issues7a9-debug.apk`

## v1.6 — Fix crash BI Global
- `GROUP BY i.tipo` na query global (crash sem finalizadas) + vazio amigável. (#6)
- APK: `ListaCompras-v1.6-biglobal-debug.apk`

## v1.5 — Form catálogo, stepper, fechar+WhatsApp, edição, ícone
- Form: nome, valor, categoria e unidade em dropdown, marca; sem código de barras; banco v3→v4. (#1)
- Stepper ≥48dp com passo por unidade. (#2) · Fechar lista + WhatsApp. (#3) · Editar item. (#4)
- `android:icon`/`roundIcon` explícitos. (#5)
- APK: `ListaCompras-v1.5-issues1a5-debug.apk`

## v1.4 — Layout, BI unificado, tema escuro, ícone e header
- Fix cards cortados + nav 6→5 abas (BI Lista+Global em TabRow) + `adjustResize`/`edge-to-edge`.
- `+ Lista` funcional, categoria separada do filtro, modo escuro via prefs, ícone adaptativo + `AppHeader`.
- APK: `ListaCompras-v1.4-5demandas-debug.apk`

## v1.1 / v1.0
- v1.1 release com shrink (~1,3MB). · v1.0 MVP debug + release assinado.
