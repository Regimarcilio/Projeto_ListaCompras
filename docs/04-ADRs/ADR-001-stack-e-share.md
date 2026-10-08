# ADR-001 — Kotlin nativo Compose + Room offline-first

Status: Aceita (2026-10-07). Contexto: app só-Android, offline no mercado, total reativo, JSON local.
Decisão: Kotlin 1.9, Compose BOM + Material3, Room (KSP) + DataStore, Navigation Compose, Hilt, Coroutines/Flow, minSdk 26/target 34.
Alternativas: PWA WebView (thespation) — descartada por performance/Intent; Flutter/KMP — descartado sem iOS.
Consequências: APK <30MB, sem INTERNET, Flow p/ acumulado, Canvas p/ BI.

# ADR-002 — Share via Intent (sem BSP pago)

Status: Aceita. Contexto: enviar lista por SMS/WhatsApp.
Decisão: `ACTION_SEND` texto + `EXTRA_STREAM` JSON via FileProvider; `ACTION_SENDTO smsto:` p/ SMS; `setPackage(com.whatsapp)` opcional.
Alternativa BSP API (custo + setup, cf. Projeto_Doctor WHATSAPP-BSP) → roadmap PJ apenas.
Consequências: custo zero, sem SEND_SMS, depende app instalado.

# ADR-003 — Snapshot + enum TipoItem para BI estável

Status: Aceita. ItemLista copia nome/tipo/unidade; TipoItem 10 valores; SUM com COALESCE.
Consequências: histórico imutável, dashboard confiável.
