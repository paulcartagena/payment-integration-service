-- Procesa los pagos en estado PENDING y registra los errores en una tabla de log.

CREATE TABLE PAYMENT_ERROR_LOG (
    ID           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    PAYMENT_ID   VARCHAR2(64),
    ERROR_CODE   NUMBER,
    ERROR_MSG    VARCHAR2(4000),
    LOGGED_AT    TIMESTAMP DEFAULT SYSTIMESTAMP
);

-- El log se escribe en una transacción autónoma: así sobrevive al ROLLBACK
-- del pago que falló, que es justamente cuando más falta hace.
CREATE OR REPLACE PROCEDURE log_payment_error(
    p_payment_id IN VARCHAR2,
    p_error_code IN NUMBER,
    p_error_msg  IN VARCHAR2
) IS
    PRAGMA AUTONOMOUS_TRANSACTION;
BEGIN
    INSERT INTO PAYMENT_ERROR_LOG (PAYMENT_ID, ERROR_CODE, ERROR_MSG)
    VALUES (p_payment_id, p_error_code, p_error_msg);
    COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE process_pending_payments(
    p_batch_size  IN  NUMBER DEFAULT 500,
    p_processed   OUT NUMBER,
    p_failed      OUT NUMBER
) IS
    CURSOR c_pending IS
        SELECT ID, CUSTOMER_ID, AMOUNT
        FROM PAYMENTS
        WHERE STATUS = 'PENDING'
        ORDER BY CREATED_AT
        FOR UPDATE SKIP LOCKED;

    TYPE t_payments IS TABLE OF c_pending%ROWTYPE;
    v_batch t_payments;
BEGIN
    p_processed := 0;
    p_failed    := 0;

    OPEN c_pending;
    LOOP
        FETCH c_pending BULK COLLECT INTO v_batch LIMIT p_batch_size;
        EXIT WHEN v_batch.COUNT = 0;

        FOR i IN 1 .. v_batch.COUNT LOOP
            BEGIN
                IF v_batch(i).AMOUNT IS NULL OR v_batch(i).AMOUNT <= 0 THEN
                    RAISE_APPLICATION_ERROR(-20001, 'Monto inválido');
                END IF;

                UPDATE PAYMENTS
                SET STATUS = 'PROCESSED'
                WHERE ID = v_batch(i).ID;

                p_processed := p_processed + 1;

            EXCEPTION
                WHEN OTHERS THEN
                    p_failed := p_failed + 1;
                    log_payment_error(v_batch(i).ID, SQLCODE, SQLERRM);
            END;
        END LOOP;

        COMMIT;
    END LOOP;
    CLOSE c_pending;

EXCEPTION
    WHEN OTHERS THEN
        IF c_pending%ISOPEN THEN
            CLOSE c_pending;
        END IF;
        log_payment_error(NULL, SQLCODE, 'Fallo general del proceso: ' || SQLERRM);
        RAISE;
END;
/