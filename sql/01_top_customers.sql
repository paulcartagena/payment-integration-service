-- Top 10 clientes por monto total pagado en los últimos 30 días.
-- Solo se consideran pagos en estado PROCESSED: son los efectivamente cobrados.

SELECT
    c.ID                          AS customer_id,
    c.NAME                        AS customer_name,
    c.COUNTRY                     AS country,
    SUM(p.AMOUNT)                 AS total_amount,
    COUNT(*)                      AS payment_count,
    ROUND(AVG(p.AMOUNT), 2)       AS average_ticket
FROM PAYMENTS p
JOIN CUSTOMERS c ON c.ID = p.CUSTOMER_ID
WHERE p.STATUS = 'PROCESSED'
AND p.CREATED_AT >= SYSDATE - 30
GROUP BY c.ID, c.NAME, c.COUNTRY
ORDER BY total_amount DESC
FETCH FIRST 10 ROWS ONLY;

-- Nota: se asume moneda única. Con múltiples monedas habría que convertir
-- a una divisa base con tipos de cambio, o agrupar también por CURRENCY.