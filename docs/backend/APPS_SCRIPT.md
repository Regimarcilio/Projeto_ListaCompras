# Backend do sync — Google Apps Script (5 min)

Recebe o lote do app (`POST {app, v, device, events}`), grava uma linha por
evento no Sheets e envia 1 e-mail ao dono no 1º evento do lote.

> E-mail de teste do dono: `extechnologies.dev@gmail.com`
> (SÓ nesta doc — NUNCA hardcoded no app; o app lê o e-mail de Ajustes).

## Passo a passo (5 min)

1. **Criar planilha:** [sheets.new](https://sheets.new) → renomeie p/ `ListaCompras-sync`.
   Cabeçalho da aba 1 (linha 1): `ts | tipo | payload`.
2. **Colar script:** Extensões → Apps Script → apague o `myFunction` e cole o
   código abaixo → salve (Ctrl+S).
3. **Implantar:** Implantar → Nova implantação → tipo **App da Web** →
   Executar como **Eu** → Acesso **Qualquer pessoa** → Implantar →
   copie a URL `https://script.google.com/macros/s/…/exec`.
4. **Colar URL no app:** Ajustes → Endpoint → cole a URL → E-mail do dono →
   `extechnologies.dev@gmail.com` → **Salvar e enviar agora**.
5. **Testar:** ative modo avião, abra o app e feche uma lista (2 eventos
   pendentes em Ajustes) → desligue o avião → fila zera e a planilha ganha
   as linhas + 1 e-mail chega ao dono.

## Script (`doPost`)

```javascript
// ListaCompras-sync — Code.gs
// Planilha destino: primeira aba, colunas A=ts, B=tipo, C=payload.

function doPost(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var body = JSON.parse(e.postData.contents || '{}');
  var events = body.events || [];
  if (!events.length) {
    return ContentService.createTextOutput(JSON.stringify({ ok: true, recebidos: 0 }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  var device = (body.device && body.device.model || '?') + ' / Android ' +
    (body.device && body.device.android || '?');
  var linhas = events.map(function (ev) {
    var data = ev.data || {};
    // Achata data p/ uma string legível: "k=v; k=v".
    var kv = Object.keys(data).map(function (k) { return k + '=' + data[k]; }).join('; ');
    return [new Date(ev.ts), ev.tipo, kv + '  [' + device + ']'];
  });
  sheet.getRange(sheet.getLastRow() + 1, 1, linhas.length, 3).setValues(linhas);

  // 1 e-mail por lote (no 1º evento), p/ não spammar o dono.
  var dono = (body.ownerEmail || 'extechnologies.dev@gmail.com');
  MailApp.sendEmail(
    dono,
    '[ListaCompras] ' + events.length + ' evento(s) sincronizados',
    'App: ' + (body.app || '?') + ' v' + (body.v || '?') + '\n' +
    'Device: ' + device + '\n' +
    'Tipos: ' + events.map(function (ev) { return ev.tipo; }).join(', ') + '\n' +
    'Ver linhas novas na planilha.'
  );
  return ContentService.createTextOutput(
      JSON.stringify({ ok: true, recebidos: events.length }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

> Nota: o app **não** envia `ownerEmail` hoje (o e-mail fica só em Ajustes,
> usado em versão futura p/ o script priorizar o destinatário). O fallback
> do script garante entrega ao dono mesmo assim.

## Validação alternativa (sem Google)

Use `https://webhook.site` como endpoint em Ajustes → **Enviar agora** →
o POST aparece no painel do webhook em segundos (prova o flush E2E).
