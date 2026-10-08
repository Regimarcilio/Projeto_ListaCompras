# 06 — Plano de Entrega + Handoff — ListaCompras

## 1. Estrutura proposta
```
Projeto_ListaCompras/
  docs/{01-visao-e-escopo.md,02-benchmarks.md,SRS.md,03-arquitetura-e-uml.md,04-ADRs/,05-backlog.md,06-plano-de-entrega.md}
  apps/mobile/ListaCompras/{settings.gradle.kts,app/build.gradle.kts,src/main/...}
  db/schemas/, seed/catalogo_seed.json
```

## 2. Setup por stack (Kotlin)
```bash
# Android Studio Ladybug+ / JDK 17
# Criar projeto: Empty Activity + Compose + minSdk 26
./gradlew build
./gradlew testDebugUnitTest connectedCheck jacocoTestReport
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 3. Branches (develop-first, adaptado: repo novo usa `develop`)
`develop` → `feature/*` → PR → `develop` → `release/*` → `main`. Commits `type(scope): desc` (feat, fix, docs, plan, test, chore). Nunca direto em main. Push `git push -u origin develop` (novo repo: criar no GitHub depois).
CI mínimo: build + unit + lint + JaCoCo gate 70%.

## 4. Checklist anti-retrabalho
- [ ] RF tem UC? Entidade tem dona? Query BI tem teste Q1-Q5?
- [ ] NFR quantificado (200ms, 2s, 70%)?
- [ ] Migration UP/DOWN + exportSchema?
- [ ] FileProvider testado em Android 10-14? Chooser com/sem WhatsApp?
- [ ] Zero Log PII, SAST limpo, TalkBack OK?
- [ ] Material CEDUP confrontado?

## 5. Melhorias sugeridas (além do pedido)
1. **Busca com autocomplete + seed 130 itens** (como Genius) — economiza digitação.
2. **Histórico de preços + alerta reposição** (como BR thespation) — "arroz subiu 12%".
3. **Duplicar/basear + listas futuras por mês** — rotina semanal.
4. **Atacado vs varejo + economia** (PWA Erick) — 2 preços por item.
5. **Backup auto + restaurar merge** — sem nuvem.
6. **Widget + voz + barcode** (roadmap).
7. **Modo dividido/conta** — dividir total por pessoa no share.

## 6. Coordenação agentes (executado)
- **datamaster:** modelo Room + JSON + queries BI (incorporado §3/§6).
- **mobilemaster:** Clean/MVVM + pastas + telas (incorporado).
- **qamaster:** 14 CTs Gherkin + automação (backlog).
- Próximos: backdev (se pedir sync), opsmaster (Play release), kalimaster (SAST/DAST), issuemaster (issues GitHub).

## 7. Handoff — próximos comandos
```bash
cd /home/project/PROJETOS/Projeto_ListaCompras
git remote add origin <url-novo-repo> && git push -u origin develop
# scaffolding: /mobilemaster-iot adapta p/ Kotlin | /datamaster-model | /qamaster-test
gh issue create --title "Sprint1 scaffold+catálogo" --body-file docs/05-backlog.md
```
Pendente: criar repo GitHub (fora do escopo CLI sem auth), confrontar Glide CEDUP, aprovar SRS/backlog para iniciar Sprint 1.
