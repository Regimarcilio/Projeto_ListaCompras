# 05 — Backlog Scrum — ListaCompras MVP

## 1. Épicos → User Stories
- **E1 Catálogo:** US01 Como comprador Quero CRUD catálogo tipado Para reutilizar (RF-001/002/008, AC CT01-05).
- **E2 Compra no corredor:** US02 Como comprador Quero ticar verde e ver total no topo Para controlar orçamento (RF-003/004/005, AC CT06-08).
- **E3 Persistência:** US03 Como comprador Quero fechar/reabrir intacto Para usar offline (RF-006, NFR-002/003, AC CT09-10).
- **E4 Export/Share:** US04 Como comprador Quero exportar JSON e compartilhar Para guardar/enviar (RF-007/011/010, AC CT11-13).
- **E5 BI:** US05 Como comprador Quero dashboard por tipo Para saber onde gasto (RF-009, AC CT14).

## 2. Sprints (velocidade inicial 8 pts/sprint, 2 devs)
- **Sprint 1 (setup + catálogo):** scaffold Hilt/Room/Nav, CatalogoScreen + seed 8 itens, DaoTest. DoR: SRS aprovado + material CEDUP confrontado. DoD: CRUD verde + cobertura ≥70%.
- **Sprint 2 (lista ativa):** ListaAtivaScreen verde + header total Flow + stepper + Turbine test + stress 500.
- **Sprint 3 (finalizar/JSON/histórico):** finalizar com data, export/import FileProvider, duplicar/basear, MigrationTest.
- **Sprint 4 (dashboard + share + polish):** BI Canvas por tipo, share chooser, A11y TalkBack, SAST, release APK.

## 3. Roadmap / marcos
M1 MVP instalável → M2 Play Internal → M3 v1.1 (filtros 7D/30D, barcode, backup auto).

## 4. Riscos sprint
Migração destrutiva, ANR, chooser vazio — mitigações no SRS §8.

## 5. Plano testes (qamaster, 14 CTs Gherkin)
CT01-05 catálogo/validação, CT06-08 seleção/total/stress, CT09-10 persist/migração, CT11-12 export, CT13 share Intent, CT14 dashboard. Automação: JUnit5+Turbine+MockK, Room inMemory, Compose Test, Espresso Intents. Matriz RF×CT no SRS §7. Gate: `./gradlew testDebugUnitTest connectedCheck jacocoTestReport` cobertura ≥70%.
