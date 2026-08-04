# Simulation of Race Condition Handling to Prevent Overselling in a Stock Management System

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/k6-Load%20Testing-7D64FF?style=for-the-badge&logo=k6&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Container-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
</p>

This project simulates a race condition on stock reduction under concurrent requests and compares it against a fix using **Pessimistic Locking (`SELECT ... FOR UPDATE`)** at the database level.

---

##  Problem & Solution Concept

### 1. The Problem: Race Condition (Overselling)

When multiple users purchase a product with limited stock at the same time:

- Thread A and Thread B read the same stock value (e.g., `stock = 1`) within the same millisecond.
- Both threads validate that stock is still available (`1 >= 1`).
- Both threads decrement the stock (`stock = stock - 1`).
- Result: stock gets decremented twice even though only 1 unit was available. The stock in the database ends up negative / oversold.

### 2. The Solution: Pessimistic Locking

Using row-level locking on PostgreSQL:

- When Thread A reads the stock using `@Lock(LockModeType.PESSIMISTIC_WRITE)`, Hibernate translates it into a `SELECT ... FOR UPDATE` query, which PostgreSQL executes to lock that row natively.
- Thread B is forced to wait until Thread A completes its transaction (commit/rollback).
- Once Thread B gets its turn, the stock has already dropped to `0`, so Thread B's transaction is immediately rejected (Stock Out / HTTP 400).

---

##  Project Structure

```text
.
├── docker-compose.yaml             # PostgreSQL container configuration
├── Makefile                        # Automation shortcuts
├── .env.example                    # Environment variables
├── k6/
│   ├── stress-test-unsafe.js       # k6 script for /buy-unsafe
│   └── stress-test-safe.js         # k6 script for /buy-safe
├── mvnw / mvnw.cmd                 # Maven Wrapper
├── pom.xml                         # Project dependencies
├── README.md                       # Project documentation
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java
    │   │   ├── domain/
    │   │   │   ├── Product.java                  # Entity model
    │   │   │   └── repository/
    │   │   │       └── ProductRepository.java    # JPA repository
    │   │   └── product/
    │   │       ├── ProductController.java        # REST controller
    │   │       ├── ProductService.java           # Business logic & transactions
    │   │       └── dto/
    │   │           ├── ProductRequest.java       # Request DTO
    │   │           └── ProductResponse.java      # Response DTO
    │   └── resources/
    │       └── application.yaml                  # Database & app config
    └── test/
        └── java/com/example/demo/
            ├── DemoApplicationTests.java
            └── product/
                ├── ProductControllerTest.java    # Controller unit test
                └── ProductServiceTest.java       # Service unit test
```

---

##  Step-by-Step Installation & Running

### 1. Clone the repository

```bash
git clone <repo-url>
cd <repo-name>
```

### 2. Start the PostgreSQL database (Docker)

Start the PostgreSQL container in the background:

```bash
docker compose up -d
```

### 3. Run the Spring Boot application

```bash
./mvnw spring-boot:run
```

### 4. Add a product to the database

Enter the database container:

```bash
docker exec -it demo-postgres psql -U postgres -d demo_db
```

Check the available tables:

```sql
\dt
```

If the `products` table exists, check its columns:

```sql
\d products
```

Insert sample data:

```sql
INSERT INTO products (name, stock)
VALUES ('Test Product', 1000);
```

Verify the data:

```sql
SELECT * FROM products;
```

Exit the database container:

```sql
\q
```

### 5. Explore the API with Swagger

Once the Spring Boot application is running, the interactive API docs are available at:

```
http://localhost:8080/swagger-ui/index.html#/
```

### 6. Run the load test

```bash
k6 run k6/stress-test-unsafe.js
k6 run k6/stress-test-safe.js
```

---

## Comparison of Real-World Benchmark Results

### * Unsafe (Pessimistic Locking)
```bash
k6 run k6/stress-test-safe.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: k6/stress-test-safe.js
        output: -

     scenarios: (100.00%) 1 scenario, 50 max VUs, 35s max duration (incl. graceful stop):
              * default: 50 looping VUs for 5s (gracefulStop: 30s)

     ✗ status is 200 (Success)
      ↳  0% — ✓ 1 / ✗ 490
     ✗ status is 400 (Stock Out)
      ↳  99% — ✓ 490 / ✗ 1
     ✓ status is NOT 404/500

     checks.........................: 66.66% ✓ 982       ✗ 491 
     data_received..................: 102 kB 19 kB/s
     data_sent......................: 86 kB  16 kB/s
     http_req_blocked...............: avg=2.24ms   min=32.38µs med=972.16µs max=50.18ms p(90)=5.36ms  p(95)=7.66ms 
     http_req_connecting............: avg=1.94ms   min=0s      med=714.98µs max=50.06ms p(90)=4.78ms  p(95)=6.95ms 
     http_req_duration..............: avg=469.27ms min=17.47ms med=354.93ms max=1.99s   p(90)=1.22s   p(95)=1.41s  
       { expected_response:true }...: avg=1.22s    min=1.22s   med=1.22s    max=1.22s   p(90)=1.22s   p(95)=1.22s  
     http_req_failed................: 99.79% ✓ 490       ✗ 1   
     http_req_receiving.............: avg=4.51ms   min=99.33µs med=1.76ms   max=37.36ms p(90)=11.58ms p(95)=17.44ms
     http_req_sending...............: avg=326.45µs min=30.6µs  med=122.55µs max=9.64ms  p(90)=883.1µs p(95)=1.32ms 
     http_req_tls_handshaking.......: avg=0s       min=0s      med=0s       max=0s      p(90)=0s      p(95)=0s     
     http_req_waiting...............: avg=464.43ms min=17.22ms med=352.43ms max=1.98s   p(90)=1.21s   p(95)=1.41s  
     http_reqs......................: 491    92.654432/s
     iteration_duration.............: avg=524.82ms min=68.92ms med=410.37ms max=2.04s   p(90)=1.27s   p(95)=1.47s  
     iterations.....................: 491    92.654432/s
     vus............................: 50     min=50      max=50

running (05.3s), 00/50 VUs, 491 complete and 0 interrupted iterations
default ✓ [======================================] 50 VUs  5s

````
### * Safe (Optimistic Locking)
```bash
k6 run k6/stress-test-safe.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: k6/stress-test-safe.js
        output: -

     scenarios: (100.00%) 1 scenario, 50 max VUs, 35s max duration (incl. graceful stop):
              * default: 50 looping VUs for 5s (gracefulStop: 30s)

     ✗ status is 200 (Success)
      ↳  0% — ✓ 1 / ✗ 490
     ✗ status is 400 (Stock Out)
      ↳  99% — ✓ 490 / ✗ 1
     ✓ status is NOT 404/500

     checks.........................: 66.66% ✓ 982       ✗ 491 
     data_received..................: 102 kB 19 kB/s
     data_sent......................: 86 kB  16 kB/s
     http_req_blocked...............: avg=2.24ms   min=32.38µs med=972.16µs max=50.18ms p(90)=5.36ms  p(95)=7.66ms 
     http_req_connecting............: avg=1.94ms   min=0s      med=714.98µs max=50.06ms p(90)=4.78ms  p(95)=6.95ms 
     http_req_duration..............: avg=469.27ms min=17.47ms med=354.93ms max=1.99s   p(90)=1.22s   p(95)=1.41s  
       { expected_response:true }...: avg=1.22s    min=1.22s   med=1.22s    max=1.22s   p(90)=1.22s   p(95)=1.22s  
     http_req_failed................: 99.79% ✓ 490       ✗ 1   
     http_req_receiving.............: avg=4.51ms   min=99.33µs med=1.76ms   max=37.36ms p(90)=11.58ms p(95)=17.44ms
     http_req_sending...............: avg=326.45µs min=30.6µs  med=122.55µs max=9.64ms  p(90)=883.1µs p(95)=1.32ms 
     http_req_tls_handshaking.......: avg=0s       min=0s      med=0s       max=0s      p(90)=0s      p(95)=0s     
     http_req_waiting...............: avg=464.43ms min=17.22ms med=352.43ms max=1.98s   p(90)=1.21s   p(95)=1.41s  
     http_reqs......................: 491    92.654432/s
     iteration_duration.............: avg=524.82ms min=68.92ms med=410.37ms max=2.04s   p(90)=1.27s   p(95)=1.47s  
     iterations.....................: 491    92.654432/s
     vus............................: 50     min=50      max=50

running (05.3s), 00/50 VUs, 491 complete and 0 interrupted iterations
default ✓ [======================================] 50 VUs  5s
```


---

##  Unit Testing

Run the unit tests for the service and controller layers:

```bash
./mvnw test
```

---

##  Cleanup

Stop the database container and remove its data volume:

```bash
docker compose down -v
```

## Notes

I'm still learning backend development, and this project was my way of understanding race conditions and overselling more 
concretely instead of just reading about them. I used Artificial Intelligence assistance along the way to help debug issues, 
structure the code, and explain concepts I hadn't worked with before, but the implementation, testing, and decisions on how 
to solve the problem are my own. Feedback and suggestions are very welcome.
