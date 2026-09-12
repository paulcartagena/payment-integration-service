# Payment Integration Service

Servicio de integración que recibe pagos en JSON desde un canal digital, los transforma al
formato XML que espera un sistema core legado y los envía.

Prueba técnica de Java e integraciones. **Tiempo de implementación: 2 h 17 min**, dentro del
timebox de 2.5 horas sugerido en el enunciado. El rango es verificable en el historial de
commits (primer commit 15:00, último 17:17).

---

## Stack

| Componente | Versión | Por qué |
|---|---|---|
| Java | 21 | LTS vigente. El enunciado pedía 11 o superior; 21 es lo que más gente tiene instalado y evita exigirle al evaluador un JDK reciente. |
| Spring Boot | 4.1.1 | Última estable al momento de resolver la prueba. La rama 3.5 ya había llegado a fin de soporte OSS. |
| Maven | wrapper incluido | No hace falta tener Maven instalado. |
| H2 | en memoria | Suficiente para la Parte 1, según indica el enunciado. |
| springdoc-openapi | 3.1.1 | Línea compatible con Spring Boot 4. |

---

## Cómo levantar el proyecto

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`.

**Credenciales:** `integration` / `integration123`

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console |

El contrato exportado también está versionado en [`docs/openapi.json`](docs/openapi.json).

### Probar los endpoints

Crear un pago:

```bash
curl -i -u integration:integration123 -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"id":"PAY-001","customerId":"CUST-42","amount":150.00,"currency":"USD","timestamp":"2026-09-12T10:00:00Z"}'
```

Devuelve `201` con header `Location`. El XML generado queda en `core-outbox/PAY-001.xml`.

Listar con filtros (los tres son opcionales y combinables):

```bash
curl -u integration:integration123 "http://localhost:8080/payments"
curl -u integration:integration123 "http://localhost:8080/payments?customerId=CUST-42"
curl -u integration:integration123 "http://localhost:8080/payments?from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z"
```

Casos de error:

```bash
# 409 — id duplicado
curl -i -u integration:integration123 -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"id":"PAY-001","customerId":"CUST-42","amount":150.00,"currency":"USD","timestamp":"2026-09-12T10:00:00Z"}'

# 400 — validaciones, devuelve todos los errores juntos
curl -i -u integration:integration123 -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"id":"","customerId":"CUST-42","amount":0,"currency":"US","timestamp":"2026-09-12T10:00:00Z"}'
```

### Tests

```bash
./mvnw test
```

---

## Estructura del repositorio

```
src/main/java/com/paulcartagena/paymentintegration/
  config/        SecurityConfig, OpenApiConfig, ClockConfig
  controller/    PaymentController
  service/       PaymentService, PaymentMapper, CoreXmlSender
  domain/        Payment, PaymentStatus
  dto/           PaymentRequest, PaymentResponse, CorePaymentXml
  repository/    PaymentRepository
  exception/     GlobalExceptionHandler, DuplicatePaymentException

sql/             Parte 2 — consulta, stored procedure e índice
scripts/         Parte 3 — script de reporte en Python
docs/            Contrato OpenAPI exportado
```

---

## Decisiones tomadas

### Tres modelos separados, no uno

`PaymentRequest` (entrada), `Payment` (persistencia) y `CorePaymentXml` (salida al core) son
clases distintas a propósito. Exponer la entidad directamente ataría el contrato público de la
API al esquema de la base: cambiar una columna rompería a todos los consumidores, y un cliente
podría enviar campos que nunca debió poder tocar, como `status`.

Los DTOs son `record` porque son inmutables por naturaleza. `Payment` es una clase porque JPA
necesita constructor sin argumentos y capacidad de mutación al hidratar desde la base.

### El formato del core es genuinamente distinto al de entrada

El XML destino no es un calco del JSON. Renombra campos (`customerId` → `CustomerRef`),
introduce estructura `Header`/`Body`, expresa la moneda como atributo XML en lugar de elemento,
y deriva un campo nuevo (`ValueDate`, el timestamp truncado a fecha en UTC). El `Header` además
contiene metadatos que no existían en la entrada.

Esto refleja lo que pasa con un core legado real y es lo que hace que la transformación sea
código con sustancia, no una reserialización.

```xml
<PaymentMessage>
    <Header>
        <MessageId>PAY-001</MessageId>
        <Channel>DIGITAL</Channel>
        <GeneratedAt>2026-09-12T22:00:00Z</GeneratedAt>
    </Header>
    <Body>
        <TransactionId>PAY-001</TransactionId>
        <CustomerRef>CUST-42</CustomerRef>
        <Amount currency="USD">150.00</Amount>
        <ValueDate>2026-09-12</ValueDate>
    </Body>
</PaymentMessage>
```

### Simulación del envío al core: archivos en disco

El XML se escribe en `core-outbox/{paymentId}.xml`, y la ruta queda registrada en la entidad.
El directorio es configurable vía `core.outbox-dir` y no se versiona, porque es salida generada
en tiempo de ejecución.

En un entorno real esto sería una cola de mensajería — lo habitual frente a un core legado —
con un outbox transaccional. El filesystem es un sustituto que mantiene la misma forma: el
servicio no sabe cómo viaja el mensaje, solo que `CoreXmlSender` se encarga.

### El envío ocurre antes de persistir

Dentro de `PaymentService.process()`, el pago se transforma y se envía al core **antes** de
guardarse con estado `SENT`. Si el envío falla, la excepción corta el método y `@Transactional`
revierte, de modo que no queda registrado un pago que el core nunca recibió.

La alternativa —persistir primero, enviar después— dejaría registros en base sin contrapartida
en el core, que es el peor de los dos estados inconsistentes posibles.

### Clave natural e idempotencia

El `id` viene del canal digital, no se genera internamente. Es `String` porque es un
identificador externo cuyo formato no controlamos, y porque no soporta operaciones aritméticas:
solo se compara por igualdad.

Usarlo como clave primaria habilita la idempotencia. Si el canal reintenta un pago —cosa que
ocurre—, el `id` repetido se detecta y se devuelve `409` en lugar de duplicar el cobro. Con una
clave sintética autogenerada, cada reintento crearía un registro nuevo.

El costo asumido: un índice sobre `VARCHAR` es más pesado que sobre `BIGINT`. A este volumen no
es relevante.

### Errores con ProblemDetail (RFC 7807)

En vez de inventar un formato propio, se usa el estándar que Spring trae de fábrica. Las
validaciones devuelven **todos** los errores juntos, no el primero que aparece, para que quien
consume la API corrija de una sola vez:

```json
{
  "type": "about:blank",
  "title": "Error de validación",
  "status": 400,
  "detail": "Uno o más campos del pago son inválidos",
  "errors": {
    "currency": "currency debe ser un código ISO-4217 de 3 letras",
    "amount": "amount debe ser mayor a cero"
  }
}
```

### Basic Authentication, no JWT

Elegido por arquitectura, no solo por tiempo: es una API de integración system-to-system, sin
usuarios finales ni sesiones. El consumidor es un canal digital con credenciales de servicio.
Montar JWT sin un flujo real de emisión, expiración y rotación agregaría ceremonia sin agregar
seguridad efectiva.

La configuración es stateless y CSRF está deshabilitado, lo cual es correcto acá: CSRF protege
contra peticiones que el navegador de un usuario autenticado emite sin su intención, escenario
que no existe cuando no hay navegador ni cookie de sesión.

Swagger y la consola H2 quedan abiertos para facilitar la revisión; en producción irían
protegidos.

### Dos campos de tiempo distintos

`timestamp` es la hora que declara el canal digital, con su offset. `receivedAt` es la hora real
de ingreso al servicio, en UTC. Los tipos difieren a propósito (`OffsetDateTime` e `Instant`):
el offset del cliente es información suya que se preserva, mientras que la marca de auditoría es
un punto absoluto en el tiempo.

Confiar en la hora del cliente para auditoría propia deja el registro a merced de relojes mal
configurados o manipulados.

### Reloj inyectado

`PaymentMapper` recibe un `Clock` en lugar de llamar a `Instant.now()`. Eso permite fijarlo en
los tests con `Clock.fixed(...)` y verificar el campo `GeneratedAt` con un valor exacto. Sin
esto, ese campo cambiaría en cada ejecución y el test de la transformación no podría cubrirlo.

### Filtros del GET con predicados opcionales

La consulta usa el patrón `:param IS NULL OR condición`, que resuelve las ocho combinaciones de
filtros con una sola query. Para tres parámetros esto es más legible que `Specification` o
Querydsl, que serían la opción correcta si el número de filtros creciera.

---

## Parte 2 — SQL y PL/SQL

Los scripts están en [`sql/`](sql/), listos para revisar. No requieren una instancia de Oracle
levantada.

| Archivo | Contenido |
|---|---|
| `01_top_customers.sql` | Top 10 clientes por monto pagado en los últimos 30 días, con cantidad de pagos y ticket promedio. |
| `02_process_pending_payments.sql` | Tabla de log y stored procedure que procesa los pagos `PENDING`. |
| `03_indexes.sql` | Índice de apoyo para la consulta anterior, con su justificación. |

Puntos destacados:

**El reporte solo cuenta pagos `PROCESSED`.** "Monto pagado" significa dinero efectivamente
cobrado. Además, si contara los `PENDING`, el total de un cliente cambiaría con solo ejecutar el
stored procedure, sin que hubiera ingresado dinero nuevo.

**El log de errores usa `PRAGMA AUTONOMOUS_TRANSACTION`.** Sin eso, el `ROLLBACK` del pago
fallido se llevaría consigo el registro del error, que es exactamente la evidencia que se
necesita conservar.

**El manejo de excepciones está dentro del loop, no al final.** Un pago inválido se registra y
se cuenta como fallido, pero el lote sigue. Con el handler solo a nivel de procedure, el primer
error abortaría todo el proceso.

**`BULK COLLECT` con `LIMIT` y `FOR UPDATE SKIP LOCKED`.** Lo primero evita el cambio de contexto
por fila entre PL/SQL y SQL sin cargar la tabla entera en memoria; lo segundo permite que dos
ejecuciones concurrentes del procedure no se pisen.

**El índice es compuesto y ordenado según el tipo de predicado:** primero la igualdad (`STATUS`),
después el rango (`CREATED_AT`), y al final las columnas que solo se leen (`CUSTOMER_ID`,
`AMOUNT`). Con las cuatro incluidas, la consulta se resuelve sin acceder a la tabla.

Una ambigüedad que quedó documentada en el propio script: el reporte asume **moneda única**. Con
múltiples monedas, sumar los montos sería incorrecto sin convertir a una divisa base.

---

## Parte 3 — Automatización con Python

```bash
cd scripts
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python payments_report.py
```

Genera `payments_summary.csv` con monto total, cantidad de pagos y promedio por cliente.

Acepta filtros opcionales que se propagan al endpoint: `--customer-id`, `--from`, `--to`, además
de `--url`, `--user`, `--password` y `--output`.

**Manejo de errores.** Las excepciones se capturan por tipo, no con un `except` genérico, porque
cada situación requiere una acción distinta de quien ejecuta el script. Los códigos de salida son
diferenciados (`1` conexión, `2` autenticación, `3` HTTP, `4` datos) para que un pipeline pueda
reintentar ante un fallo de red pero alertar de inmediato ante credenciales inválidas. Todas las
llamadas HTTP tienen `timeout` explícito.

```bash
$ python payments_report.py --password incorrecta
Credenciales inválidas. Revisá el usuario y la password.

$ python payments_report.py --url http://localhost:9999
No se pudo conectar con la API en http://localhost:9999. Verificá que esté levantada.
```

**`Decimal`, no `float`**, por la misma razón que `BigDecimal` en Java. La conversión pasa por
`str()` para no arrastrar el error binario que se quería evitar.

---

## Qué dejaría para una siguiente iteración

Lo siguiente quedó fuera por decisión de alcance, no por olvido:

**Envío real al core con outbox transaccional.** Reemplazar la escritura en disco por una cola,
con una tabla de outbox escrita en la misma transacción que el pago y un proceso aparte que la
drena. Es la forma correcta de garantizar que no se pierda ni se duplique un mensaje.

**Reintentos con backoff exponencial** ante fallos transitorios del core, con un límite de
intentos y una dead letter queue. Hoy un fallo de envío revierte la transacción y el pago
simplemente no se registra.

**Migraciones versionadas con Flyway.** `ddl-auto: update` es aceptable sobre una base en memoria
efímera, pero no es una estrategia de esquema para un entorno real.

**Paginación en `GET /payments`.** Hoy devuelve la lista completa. Con volumen real haría falta
paginar y probablemente ordenar de forma configurable.

**Validación de `currency` contra la lista ISO-4217 completa.** La validación actual es de
formato: acepta tres letras mayúsculas, de modo que `XYZ` pasaría.

**Credenciales fuera del código.** Están fijas en `SecurityConfig` para que la prueba sea
reproducible sin configuración previa. En un entorno real irían en variables de entorno o en un
gestor de secretos.

**Más cobertura de tests.** La transformación está cubierta, que es lo que el enunciado pedía
como mínimo. Faltarían tests de integración sobre los endpoints (`MockMvc`) y sobre la capa de
repositorio.

**Observabilidad.** Métricas, trazas y logs estructurados con correlación por `paymentId`.