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

```bash
# Criar uma filial
curl -X POST http://localhost:8080/api/branches \
  -H "Content-Type: application/json" \
  -d '{"name":"Doca Central","location":"BR-163, km 42"}'

# Criar um caminhão (com grão atribuído)
curl -X POST http://localhost:8080/api/trucks \
  -H "Content-Type: application/json" \
  -d '{"plate":"ABC1D23","tareWeight":8500.0,"grainId":"<UUID_DO_GRÃO>"}'

# Criar um grão
curl -X POST http://localhost:8080/api/grains \
  -H "Content-Type: application/json" \
  -d '{"name":"Soja","purchasePricePerTon":1850.00}'

# Criar uma balança (usar o ID da filial retornado acima)
curl -X POST http://localhost:8080/api/scales \
  -H "Content-Type: application/json" \
  -d '{"externalId":"ESP32-DOCK-01","branchId":"<UUID_DA_FILIAL>"}'
```

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

```bash
# Simular 35 leituras (~3.5 segundos) com peso estável
for i in $(seq 1 35); do
  curl -s -X POST http://localhost:8080/api/weighing \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"ESP32-DOCK-01\",\"plate\":\"ABC1D23\",\"weight\":25430.5}"
  sleep 0.1
done
```

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

### Margem de lucro dinâmica

A margem varia de **5% a 20%** baseada na escassez do grão:

| Transações existentes | Margem aplicada |
|:---------------------:|:---------------:|
| 0 (grão escasso) | 20% |
| 5 | 12.5% |
| 10+ (abundante) | 5% |

### Teste completo (ponta a ponta)

```bash
# 1. Subir Redis e rodar a app
docker compose up -d
./mvnw spring-boot:run

# 2. Criar dados base
BRANCH=$(curl -s -X POST http://localhost:8080/api/branches \
  -H "Content-Type: application/json" \
  -d '{"name":"Doca Central","location":"BR-163"}' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

GRAIN=$(curl -s -X POST http://localhost:8080/api/grains \
  -H "Content-Type: application/json" \
  -d '{"name":"Soja","purchasePricePerTon":1850.00}' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST http://localhost:8080/api/scales \
  -H "Content-Type: application/json" \
  -d "{\"externalId\":\"ESP32-DOCK-01\",\"branchId\":\"$BRANCH\"}"

curl -s -X POST http://localhost:8080/api/trucks \
  -H "Content-Type: application/json" \
  -d "{\"plate\":\"ABC1D23\",\"tareWeight\":8500.0,\"grainId\":\"$GRAIN\"}"

# 3. Simular pesagem (35 leituras, ~3.5 segundos)
for i in $(seq 1 35); do
  curl -s -X POST http://localhost:8080/api/weighing \
    -H "Content-Type: application/json" \
    -d '{"id":"ESP32-DOCK-01","plate":"ABC1D23","weight":25430.5}'
  sleep 0.1
done

# 4. Verificar a transação processada (aguardar ~2 segundos para o consumer)
sleep 2
curl -s http://localhost:8080/api/transactions | python3 -m json.tool
```
