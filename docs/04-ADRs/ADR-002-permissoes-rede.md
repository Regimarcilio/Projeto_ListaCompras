# ADR-002 — Permissões de rede p/ sync outbox (revogação parcial da NFR-005)

Status: Aceita (issue #22).

## Contexto

A NFR-005 original vedava `INTERNET` (app 100% offline, sem coleta).
As issues #21/#22 introduziram sync online: eventos locais (outbox Room)
são enviados a uma base externa (Google Apps Script → Sheets + e-mail ao dono)
quando há conexão, configurável em Ajustes.

## Decisão

- Adicionar ao `AndroidManifest`:
  - `INTERNET` — POST https do lote de eventos via `HttpURLConnection`;
  - `ACCESS_NETWORK_STATE` — constraint `NetworkType.CONNECTED` do WorkManager.
- Endpoint + e-mail do dono ficam em DataStore (PrefsRepo), vazios por padrão:
  sem endpoint configurado, nenhum tráfego ocorre (`SyncWorker` retorna
  `Result.success()` sem POST).
- Só `https://` é aceito (http recusado em `SyncPayload.urlValida`).

## Revogação parcial

A NFR-005 segue válida no restante: sem `SEND_SMS`, sem `LOCATION`,
sem coleta de face/localização (LGPD), sem polling (WorkManager sob
demanda + constraint de rede, com backoff em retry).

## Consequências

- Tráfego mínimo: 1 POST por flush, lote ≤200 eventos, timeout 15s.
- Privacidade: payload contém só `{tipo, ts, data}` de uso do app
  (`app_aberto`, `lista_fechada`, `login` futuro #20) — sem dados sensíveis.
