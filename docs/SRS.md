# SRS v1.0 — ListaCompras Supermercado (Android Nativo Kotlin)

> Stack: Kotlin 1.9, Compose BOM, Material3, Room, DataStore, Navigation, Hilt, minSdk 26 / target 34. Offline-first, sem INTERNET.

## 1. Introdução
### 1.1 Propósito
Especificar app que carrega lista ampla de supermercado, permite adicionar item avulso (valor+tipo), ticar com verde + total acumulado, salvar JSON datado, reutilizar catálogo, dashboard por tipo e compartilhar via SMS/WhatsApp.
### 1.2 Escopo
Ver `docs/01-visao-e-escopo.md`. IN: catálogo, lista ativa, Room, JSON, BI por tipo, share Intent. OUT: nuvem/login/sync.
### 1.3 Definições
Catálogo = item reutilizável. Lista = instância de compra. ItemLista = snapshot (nome/tipo/unidade copiados). Selecionado = ticado no mercado (verde). Acumulado = SUM(preco*qtd WHERE selecionado).
### 1.4 Referências
`01-visao-e-escopo.md`, `02-benchmarks.md`, GroceryGenius (GPL-3.0, referência sem copiar código copyleft sem abrir fonte), Room docs, material CEDUP https://integrador-cedup.glide.page/dl/a400f7.

## 2. Descrição geral
Single-Activity + 4 rotas (Catálogo, ListaAtiva, Dashboard, Histórico). Room fonte da verdade, UI observa Flow. Atores: Comprador (único). Premissas: sem rede no mercado; sem conta; dados locais.

## 3. Requisitos funcionais (MoSCoW + Gherkin)

| ID | Título | Ator | Pri | Descrição | Critério de aceite (Gherkin) | Dep |
|---|---|---|---|---|---|---|
| RF-001 | CRUD catálogo tipado | Comprador | Must | Criar/editar/inativar item com nome, tipo enum (10 valores), unidade, precoRef | Dado catálogo vazio Quando cadastro Banana prata HORTIFRUTI kg 5.99 Então lista exibe e buscar "bana" retorna 1 | — |
| RF-002 | Adicionar item avulso | Comprador | Must | Se não há na lista, criar com nome+preço corrente+tipo+unidade+qtd; vira catálogo opcional | Dado "Picanha" não existe Quando adiciono 1.5kg CARNE 64.90 Então item aparece e catálogo sugere salvar | RF-001 |
| RF-003 | Montar lista a partir do catálogo | Comprador | Must | Buscar/filtrar por tipo, adicionar com qtd/preço (default precoRef), ordenar | Dado catálogo com 8 seeds Quando filtro CARNE e adiciono Patinho Então lista contém 1 item | RF-001 |
| RF-004 | Seleção verde + total acumulado | Comprador | Must | Tap checkbox alterna selecionado; card verde (`greenContainer`), header fixo Total R$ + N itens reativo <200ms | Dado Arroz 20.00 e Sabão 10.50 não sel total 0 Quando marco ambos Então total 30.50 e ambos verdes | RF-003 |
| RF-005 | Editar qtd/preço no corredor | Comprador | Must | Stepper qtd, edit preço; total recalcula; permite preco null (sem preço) | Dado item 2kg 5.99 Quando mudo para 3kg Então total +5.99 | RF-004 |
| RF-006 | Salvar/finalizar lista com data | Comprador | Must | Finalizar grava dataCompra ISO-8601 + epoch, gera JSON em `cache/export/` + Downloads | Dado lista com 3 itens Quando finalizo 05/10 11:20 Então histórico mostra e JSON contém dataCompra | RF-004 |
| RF-007 | Export/Import JSON | Comprador | Must | Export `schemaVersion`, `exportedAt`, listas+itens; import valida e relata erros; FileProvider | Dado 2 produtos Quando exporto Então arquivo listacompras-*.json válido; Quando importo corrompido Então erro sem apagar dados | RF-006 |
| RF-008 | Catálogo persistente reutilizável | Comprador | Must | Todo avulso pode virar catálogo; inativar sem apagar histórico (snapshot) | Dado item avulso Quando toco "salvar no catálogo" Então futura busca encontra | RF-002 |
| RF-009 | Dashboard BI por tipo | Comprador | Must | Barras por tipo (SUM preco*qtd), total geral, ticket médio; filtro por lista e global finalizadas | Dado Arroz Mercado 20 sel + Sabão Limpeza 10 sel + Bala 5 não sel Quando abro dashboard Então Mercado 20 Limpeza 10 total 30 | RF-004 |
| RF-010 | Histórico + duplicar/basear | Comprador | Must | Listar por data desc, duplicar (com/sem zerar preços), basear nova em anterior | Dado lista finalizada Quando duplico zerando Então nova lista mesmos nomes preços null | RF-006 |
| RF-011 | Compartilhar SMS/WhatsApp | Comprador | Must | `ACTION_SEND` texto formatado + `EXTRA_STREAM` JSON via FileProvider, chooser; `setPackage(com.whatsapp)` opcional; `ACTION_SENDTO smsto:` | Dado JSON exportado Quando compartilhar Então chooser abre com WhatsApp/SMS | RF-007 |
| RF-012 | Busca/filtros/ordenação | Comprador | Should | Busca case-insensitive, chips tipo, ordenação nome/tipo/preço | Dado 50 itens Quando busco "arroz" Então filtra em <300ms | RF-001 |
| RF-013 | Preferências/tema | Comprador | Could | DataStore: tema, filtro default, último export | Dado troco para escuro Quando reabro Então mantém | — |

## 4. Requisitos não-funcionais

| ID | Categoria | Especificação quantificada |
|---|---|---|
| NFR-001 | Performance | Seleção atualiza header <200ms; 500 itens toggle-all <2s sem ANR; busca <300ms |
| NFR-002 | Offline | 100% funcional sem rede; Room + DataStore; sem INTERNET no manifest |
| NFR-003 | Persistência | Room exportSchema=true, migrations versionadas, nunca destructive em release; kill/restart preserva |
| NFR-004 | Usabilidade/A11y | Contraste ≥4.5, contentDescription checkbox/total, fonte 200% sem corte, TalkBack |
| NFR-005 | Segurança/Privacidade | Sem permissão perigosa; FileProvider grant read; zero Log com nome/preço; LGPD n/a (só local) |
| NFR-006 | Compatibilidade | minSdk 26, target 34, Compose BOM, KSP; APK release <30MB |
| NFR-007 | Manutenibilidade | Clean+MVVM, domain puro, cobertura ≥70% novo, JaCoCo gate CI |

## 5. Interfaces / integrações
- UI Compose Material3 (4 telas). Sem API externa.
- Share: `Intent.ACTION_SEND type text/plain` (texto `• nome — qtd x preço = subtotal`) + `application/json` com URI FileProvider (`<cache-path path="export/"/>`, `FLAG_GRANT_READ_URI_PERMISSION`).
- Armazenamento: `cacheDir/export/lista_<id>.json` + cópia MediaStore/Downloads via SAF.
- Exemplo JSON: ver `docs/03-arquitetura-e-uml.md §6` (datamaster).

## 6. Regras de negócio
- RN-001 Snapshot: ItemLista copia nome/tipo/unidade; catálogo pode mudar sem corromper BI.
- RN-002 Tipo fechado: 10 valores enum; avulso exige tipo (default OUTROS).
- RN-003 Preço: `precoUnit>=0` ou null (sem preço = 0 no SUM via COALESCE, listado em Q5 higiene).
- RN-004 Quantidade >0.
- RN-005 Finalizar exige ≥1 item; gera dataCompra; congela edição (só duplicar).
- RN-006 Inativar catálogo nunca apaga histórico.
- RN-007 Export inclui `exportedAt` + `schemaVersion`; import rejeita `schemaVersion>atual`.

## 7. Matriz rastreabilidade RF x RN x US x Teste
| RF | RN | US | Teste (qamaster) |
|---|---|---|---|
| RF-001,002,008 | RN-002,003,004,006 | US01 | CT01-05 (DaoTest+Compose) |
| RF-003,004,005 | RN-003 | US02 | CT06-08 (Turbine+stress 500) |
| RF-006,007,010 | RN-005,007 | US03/04 | CT09-12 (Migration+Export) |
| RF-011 | — | US04 | CT13 (Intents ACTION_SEND) |
| RF-009 | RN-001 | US05 | CT14 (Dashboard por tipo) |

## 8. Riscos e mitigações
- GPL copyleft (Genius) → não copiar código, só arquitetura; licença própria MIT.
- Perda dados migração → exportSchema + Migration UP/DOWN + teste 1→2.
- ANR 500 itens → Flow + Dispatchers.IO + `key=id` em LazyColumn.
- Share sem app alvo → chooser + fallback copiar área transferência.
- Divergência material CEDUP → revisão conjunta antes Sprint 1.

## 9. Glossário
Acumulado, Snapshot, Seed, FileProvider, Intent chooser, Ticket médio, BI por tipo.
