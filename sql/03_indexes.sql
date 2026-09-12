-- Índice de apoyo para la consulta de top clientes (01_top_customers.sql).
--
-- La consulta filtra por STATUS y por rango de CREATED_AT, y luego agrupa
-- por CUSTOMER_ID sumando y promediando AMOUNT.
--
-- El orden de las columnas responde a eso:
--   STATUS      va primero por ser un predicado de igualdad. Oracle puede
--               posicionarse directamente en la rama del índice y todo lo
--               que sigue queda acotado a los pagos PROCESSED.
--   CREATED_AT  va segundo porque es un predicado de rango. Una columna de
--               rango deja de acotar la búsqueda para las columnas que vengan
--               después, así que tiene que ir detrás de todas las igualdades.
--   CUSTOMER_ID y AMOUNT no filtran nada: se incluyen para que el índice
--               contenga todos los datos que la consulta necesita.
--
-- Ese último punto es el beneficio principal. Con las cuatro columnas en el
-- índice, Oracle resuelve la consulta recorriendo solo el índice y nunca
-- accede a la tabla. Se evita el acceso por ROWID, que en una tabla grande
-- es la parte más cara del plan.

CREATE INDEX IX_PAYMENTS_STATUS_CREATED
    ON PAYMENTS (STATUS, CREATED_AT, CUSTOMER_ID, AMOUNT);

-- Contrapartida: todo índice se mantiene en cada INSERT, UPDATE y DELETE.
-- PAYMENTS es una tabla de alta escritura y este índice incluye STATUS, que
-- el procedure del punto 2 modifica en cada pago. Es un costo real y asumido:
-- el reporte se ejecuta seguido y sin el índice haría full scan de la tabla.