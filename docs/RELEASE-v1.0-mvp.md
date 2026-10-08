# Release v1.0-mvp — ListaCompras

## Como gerar o APK
```bash
cd apps/mobile/ListaCompras
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

## Checklist release
- [x] Ícone adaptativo + tema + proguard
- [x] Export Downloads (MediaStore) + import SAF + backup auto semanal (DataStore)
- [x] 6 telas: Catálogo, Lista (verde+total), BI, Histórico, Global 7D/30D, Ajustes
- [x] Testes: ValidacaoTest + JsonCodecTest + Sprint3Test
- [ ] Rodar `./gradlew testDebugUnitTest connectedCheck` no Android Studio
- [ ] Publicar Internal Testing na Play (assinatura + privacy policy: 100% local)

## Formato share WhatsApp/SMS
```
🛒 Compra semanal 05/10
• Banana prata — 2.0x R$ 5.99 = R$ 11.98
• Patinho — 1.5x R$ 42.90 = R$ 64.35
TOTAL: R$ 76.33
```
+ anexo `listacompras-<ts>.json`
