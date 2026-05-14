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
```
# 3. Le as mensagens do Redis Stream

docker exec -it esp32-redis-1 redis-cli XRANGE weighing:stabilized - +
```

A aplicação sobe na porta **8080**. Console do H2 disponível em: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:grainscale`, user: `sa`, sem senha).

---

## Etapa 1 — Modelagem e Cadastros ✅

Estrutura em camadas (Layered Architecture) com cadastros CRUD para as entidades do sistema.

### Entidades

| Entidade | Descrição |
|----------|-----------|
| **Truck** | Caminhão — placa e peso da tara (kg) |
| **Grain** | Tipo de grão — nome e preço de compra por tonelada |
| **Branch** | Filial/Doca — ponto de operação |
| **Scale** | Balança (ESP32) — vinculada a uma filial |
| **Transaction** | Transação de pesagem — resultado final (usada nas Etapas 2 e 3) |

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

### Exemplos de uso

```bash
# Criar uma filial
curl -X POST http://localhost:8080/api/branches \
  -H "Content-Type: application/json" \
  -d '{"name":"Doca Central","location":"BR-163, km 42"}'

# Criar um caminhão
curl -X POST http://localhost:8080/api/trucks \
  -H "Content-Type: application/json" \
  -d '{"plate":"ABC1D23","tareWeight":8500.0}'

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


