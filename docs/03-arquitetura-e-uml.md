# 03 — Arquitetura e UML — ListaCompras

> Clean + MVVM, Single-Activity, Room fonte da verdade. Contribuições: datamaster (modelo) + mobilemaster (pastas).

## 1. Visão componentes
- `presentation -> domain <- data`; domain puro-Kotlin.
- Telas: Catálogo, ListaAtiva (verde+total topo), Dashboard (Canvas barras), Histórico (export/share).

```mermaid
flowchart LR
  UI1[CatalogoScreen] --> VM1[CatalogoViewModel]
  UI2[ListaAtivaScreen] --> VM2[ListaAtivaViewModel]
  UI3[DashboardScreen] --> VM3[DashboardViewModel]
  UI4[HistoricoScreen] --> VM4[HistoricoViewModel]
  VM1 & VM2 & VM3 & VM4 --> DOM[domain use-cases + repos]
  DOM --> DATA[data RepositoryImpl + Room + DataStore]
  VM4 --> EXP[FileProvider + Share Intent]
  DATA --> DB[(Room: catalogo_item, lista_compra, item_lista)]
```

## 2. Casos de uso
```mermaid
flowchart TD
  A[Comprador] --> UC1[(CRUD catalogo)]
  A --> UC2[(Adicionar avulso)]
  A --> UC3[(Montar lista)]
  A --> UC4[(Ticar verde + ver total)]
  A --> UC5[(Finalizar + JSON datado)]
  A --> UC6[(Ver BI por tipo)]
  A --> UC7[(Compartilhar SMS WhatsApp)]
  A --> UC8[(Duplicar basear historico)]
  UC2 -.-> UC1
  UC4 --> UC5
  UC5 --> UC6
  UC5 --> UC7
```

## 3. Classes / ER (Room)
```mermaid
erDiagram
  CATALOGO_ITEM ||--o{ ITEM_LISTA : origina
  LISTA_COMPRA ||--|{ ITEM_LISTA : contem
  CATALOGO_ITEM {
    TEXT id PK
    TEXT nome UK
    TEXT tipo
    TEXT unidadeDefault
    REAL precoRef
  }
  LISTA_COMPRA {
    TEXT id PK
    TEXT nome
    INTEGER dataCriacao_epoch
    TEXT dataCompra_ISO
    INTEGER finalizada
  }
  ITEM_LISTA {
    TEXT id PK
    TEXT listaId FK
    TEXT catalogoItemId FK_NULL
    TEXT nome_snapshot
    TEXT tipo_snapshot
    REAL quantidade
    REAL precoUnit
    INTEGER selecionado
  }
```
Entidades Kotlin + DAOs + `TotalPorTipo` + `acumuladoSelecionados()` + `totalPorTipo()` — ver detalhamento datamaster arquivado neste doc (snapshot 3FN, índices listaId/tipo/selecionado, Migration 1→2 sem destructive).

## 4. Sequência — fluxo principal (ticar no mercado)
```mermaid
sequenceDiagram
  participant U as Comprador
  participant S as ListaAtivaScreen
  participant V as ListaAtivaViewModel
  participant UC as ToggleSelecaoUseCase
  participant R as ListaRepository
  participant D as Room item_lista
  U->>S: tap checkbox Patinho
  S->>V: onToggle(id, true)
  V->>UC: invoke(id, true)
  UC->>R: setSelecionado + updatedAt
  R->>D: UPDATE selecionado
  D-->>S: Flow<List> + Flow<Double acumulado> reemite
  S-->>U: card verde + header Total R$76.33
```

## 5. Sequência — finalizar/export/share
```mermaid
sequenceDiagram
  participant U as Comprador
  participant H as HistoricoScreen
  participant E as ExportJsonUseCase
  participant F as FileProvider
  U->>H: Finalizar lista
  H->>E: serialize Room -> JSON + exportedAt
  E->>F: cache/export/lista_id.json -> content URI
  F-->>U: ACTION_SEND chooser WhatsApp SMS
```

## 6. Exemplo JSON (schema v2)
```json
{
  "app": "ListaCompras", "schemaVersion": 2,
  "exportedAt": "2026-10-07T14:30:00-03:00",
  "listas": [{
    "id": "lst_01J9", "nome": "Compra semanal 05/10",
    "dataCriacao": 1728145200000, "dataCompra": "2026-10-05T11:20:00-03:00",
    "finalizada": true,
    "itens": [
      {"id":"it_001","nome":"Banana prata","tipo":"HORTIFRUTI","unidade":"kg","quantidade":2.0,"precoUnit":5.99,"selecionado":true,"ordem":0},
      {"id":"it_002","nome":"Patinho","tipo":"CARNE","unidade":"kg","quantidade":1.5,"precoUnit":42.90,"selecionado":true,"ordem":1}
    ]
  }]
}
```

## 7. Estrutura pastas (Gradle Kotlin DSL)
`apps/mobile/ListaCompras/app/src/main/java/br/com/listacompras/{data/local/{AppDatabase,dao,entity,prefs}, data/repository, domain/{model,repository,usecase}, presentation/{navigation,theme,catalogo,listaativa,dashboard,historico}, share/{ExportFileProvider,ShareHelper}}` + `res/xml/filepaths.xml` + `gradle/libs.versions.toml`. Manifest sem INTERNET.

## 8. Validação consistência
Todo RF tem UC e entidade dona; toda query BI tem teste (Q1-Q5 datamaster); nenhum dado sem dono (catálogo→CatalogoDao, lista→ListaDao).
