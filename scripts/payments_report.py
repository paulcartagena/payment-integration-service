#!/usr/bin/env python3
"""
Consume GET /payments y genera un CSV con el resumen por cliente:
monto total, cantidad de pagos y promedio.
"""

import argparse
import csv
import sys
from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP

import requests
from requests.auth import HTTPBasicAuth
from requests.exceptions import ConnectionError, HTTPError, Timeout

TIMEOUT_SECONDS = 10

EXIT_OK = 0
EXIT_CONNECTION = 1
EXIT_AUTH = 2
EXIT_HTTP = 3
EXIT_DATA = 4


def fetch_payments(base_url, user, password, customer_id=None, date_from=None, date_to=None):
    params = {}
    if customer_id:
        params["customerId"] = customer_id
    if date_from:
        params["from"] = date_from
    if date_to:
        params["to"] = date_to

    response = requests.get(
        f"{base_url.rstrip('/')}/payments",
        auth=HTTPBasicAuth(user, password),
        params=params,
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def summarize(payments):
    """Agrupa por cliente. Usa Decimal para no arrastrar errores de punto flotante."""
    totals = defaultdict(lambda: {"total": Decimal("0"), "count": 0})

    for payment in payments:
        customer_id = payment["customerId"]
        totals[customer_id]["total"] += Decimal(str(payment["amount"]))
        totals[customer_id]["count"] += 1

    rows = []
    for customer_id, data in sorted(totals.items()):
        average = (data["total"] / data["count"]).quantize(Decimal("0.01"), ROUND_HALF_UP)
        rows.append({
            "customer_id": customer_id,
            "total_amount": data["total"].quantize(Decimal("0.01"), ROUND_HALF_UP),
            "payment_count": data["count"],
            "average_amount": average,
        })

    return rows


def write_csv(rows, output_path):
    fieldnames = ["customer_id", "total_amount", "payment_count", "average_amount"]
    with open(output_path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def parse_args():
    parser = argparse.ArgumentParser(description="Genera un resumen de pagos por cliente en CSV.")
    parser.add_argument("--url", default="http://localhost:8080", help="URL base de la API")
    parser.add_argument("--user", default="integration", help="Usuario de la API")
    parser.add_argument("--password", default="integration123", help="Password de la API")
    parser.add_argument("--customer-id", help="Filtra por un cliente puntual")
    parser.add_argument("--from", dest="date_from", help="Fecha desde, en formato ISO-8601")
    parser.add_argument("--to", dest="date_to", help="Fecha hasta, en formato ISO-8601")
    parser.add_argument("--output", default="payments_summary.csv", help="Archivo CSV de salida")
    return parser.parse_args()


def main():
    args = parse_args()

    try:
        payments = fetch_payments(
            args.url, args.user, args.password,
            args.customer_id, args.date_from, args.date_to,
        )
    except ConnectionError:
        print(f"No se pudo conectar con la API en {args.url}. Verificá que esté levantada.", file=sys.stderr)
        return EXIT_CONNECTION
    except Timeout:
        print(f"La API no respondió en {TIMEOUT_SECONDS} segundos.", file=sys.stderr)
        return EXIT_CONNECTION
    except HTTPError as exc:
        status = exc.response.status_code
        if status == 401:
            print("Credenciales inválidas. Revisá el usuario y la password.", file=sys.stderr)
            return EXIT_AUTH
        if status == 403:
            print("El usuario no tiene permisos sobre este recurso.", file=sys.stderr)
            return EXIT_AUTH
        print(f"La API respondió con error {status}.", file=sys.stderr)
        return EXIT_HTTP
    except ValueError:
        print("La respuesta de la API no es JSON válido.", file=sys.stderr)
        return EXIT_DATA

    if not payments:
        print("No se encontraron pagos con los filtros indicados.")
        return EXIT_OK

    try:
        rows = summarize(payments)
    except (KeyError, TypeError) as exc:
        print(f"La respuesta no tiene el formato esperado: {exc}", file=sys.stderr)
        return EXIT_DATA

    write_csv(rows, args.output)
    print(f"{len(rows)} clientes resumidos a partir de {len(payments)} pagos en {args.output}")
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main())