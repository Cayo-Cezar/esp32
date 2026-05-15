# 🚛 Grain Scale — Sistema de Pesagem de Transporte de Grãos

Sistema para ingestão, estabilização e armazenamento de dados de pesagem de caminhões de grãos, utilizando ESP32 e câmeras LPR.

## Tecnologias

- **Java 21** + **Spring Boot 3.3**
- **Spring Data JPA** + **H2** (banco em memória)
- **Bean Validation** (validação de entrada)
- **Redis Streams** (mensageria para dados estabilizados)
- **Docker Compose** (infraestrutura)

## Como Rodar

```bash
# 1. Subir o Redis
docker compose up -d

# 2. Rodar a aplicação
./mvnw spring-boot:run

# 3. Ler as mensagens do Redis Stream
docker exec -it esp32-redis-1 redis-cli XRANGE weighing:stabilized - +
```

A aplicação sobe na porta **8080**. Console do H2 disponível em: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:grainscale`, user: `sa`, sem senha).

---

## Etapa 1 — Modelagem e Cadastros ✅

Estrutura em camadas (Layered Architecture) com cadastros CRUD para as entidades do sistema.

### Entidades

| Entidade | Descrição |
|----------|-----------|
| **Truck** | Caminhão — placa, peso da tara (kg) e grão atribuído |
| **Grain** | Tipo de grão — nome e preço de compra por tonelada |
| **Branch** | Filial/Doca — ponto de operação |
| **Scale** | Balança (ESP32) — vinculada a uma filial |
| **Transaction** | Transação de pesagem — placa, pesos, preços, margem e status |

### Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/trucks` | Cadastrar caminhão |
| `GET` | `/api/trucks` | Listar caminhões |
| `GET` | `/api/trucks/{id}` | Buscar por ID |
| `GET` | `/api/trucks/plate/{plate}` | Buscar por placa |
| `POST` | `/api/grains` | Cadastrar grão |
| `GET` | `/api/grains` | Listar grãos |
| `GET` | `/api/grains/{id}` | Buscar por ID |
| `POST` | `/api/branches` | Cadastrar filial |
| `GET` | `/api/branches` | Listar filiais |
| `GET` | `/api/branches/{id}` | Buscar por ID |
| `POST` | `/api/scales` | Cadastrar balança |
| `GET` | `/api/scales` | Listar balanças |
| `GET` | `/api/scales/{id}` | Buscar por ID |
| `GET` | `/api/scales/external/{externalId}` | Buscar por ID do ESP32 |
| `GET` | `/api/transactions` | Listar transações |
| `GET` | `/api/transactions/{id}` | Buscar transação por ID |
| `GET` | `/api/transactions/grain/{grainId}` | Transações por grão |
| `GET` | `/api/reports/summary` | Resumo geral (totais, lucro, margem média) |
| `GET` | `/api/reports/by-grain` | Relatório por tipo de grão |
| `GET` | `/api/reports/by-branch` | Relatório por filial |
| `GET` | `/api/reports/by-truck/{plate}` | Relatório por caminhão |

### Exemplos de uso

Utilize o arquivo **`Grain_Scale_API.postman_collection.json`** presente na raiz do projeto para realizar os testes de todos os endpoints. Ele já vem configurado com as requisições na ordem correta do fluxo da aplicação.

---

## Etapa 2 — Ingestão e Estabilização ✅

Endpoint de ingestão de dados do ESP32 com algoritmo **Sliding Window** para estabilização do peso.

### Como funciona

```
ESP32 (POST a cada 100ms)  →  WeighingController  →  WeighingService (Sliding Window)
                                                          │
                                                          ├── peso estável (≥3s, variância baixa) → publica no Redis Stream
                                                          └── fuga prematura (<3s) → descarta tudo
```

1. O ESP32 envia `POST /api/weighing` com `{ "id", "plate", "weight" }` a cada 100ms
2. O `WeighingService` acumula as leituras numa **janela deslizante** em memória
3. Quando a janela tem **≥ 3 segundos** de dados e a **variância é baixa** → peso estabilizado
4. O peso médio é publicado no **Redis Stream** (`weighing:stabilized`)
5. Se o caminhão **sair antes de 3 segundos** → fuga prematura → nada é publicado

### Endpoint

| Método | Endpoint | Descrição |
|--------|----------|------ |
| `POST` | `/api/weighing` | Recebe leitura do ESP32 (retorna 202 Accepted) |

### Exemplo — Simular pesagem

A simulação e testes devem ser executados através da **Collection do Postman** fornecida no repositório (`Grain_Scale_API.postman_collection.json`). Ela contém todas as requisições de cadastros e o endpoint de simulação para gerar pesagens consecutivas de forma rápida.

---

## Etapa 3 — Worker e Regras de Negócio ✅

Consumer que lê o Redis Stream, aplica regras de negócio e salva a transação no banco.

### Fluxo

```
Redis Stream (weighing:stabilized)
        ↓  consumer poll a cada 1s
  TransactionService
        ↓
  1. Busca caminhão pela placa → pega tara e grainId
  2. Busca balança pelo externalId → pega branchId
  3. Busca grão pelo ID → pega preço/ton
  4. Calcula peso líquido = bruto - tara
  5. Calcula margem de lucro dinâmica (5% a 20%)
  6. Calcula preço de compra e venda
  7. Salva Transaction no banco com status PROCESSED
```

### Margem de lucro dinâmica e Controle de Estoque

A margem varia de **5% a 20%** e é inversamente proporcional à quantidade disponível (estoque) do grão na filial.
Foi adicionada a entidade **Inventory** para registrar este controle:

| Estoque existente na doca | Margem aplicada |
|:-------------------------:|:---------------:|
| 0 kg (grão escasso) | 20% |
| 50.000 kg (50 Toneladas) | 12.5% |
| >= 100.000 kg (abundante) | 5% |

### Teste completo (ponta a ponta)

Para testar o fluxo de ponta a ponta, utilize o arquivo **`Grain_Scale_API.postman_collection.json`**. Nele você encontra:
1. Cadastros de Filial, Grão, Caminhão e Balança.
2. Endpoint simulador para injetar 35 leituras sequenciais.
3. Endpoints de consultas das Transações e Relatórios processados.
