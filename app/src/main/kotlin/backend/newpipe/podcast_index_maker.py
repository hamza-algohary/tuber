import sqlite3
import csv
import argparse
import os
import sys

def parse_args():
    parser = argparse.ArgumentParser(
        description="Export SQLite table to CSV with optional filters"
    )

    parser.add_argument(
        "--db",
        required=True,
        help="Path to SQLite database file"
    )

    parser.add_argument(
        "--table",
        required=True,
        help="Table name to export"
    )

    parser.add_argument(
        "--columns",
        default="*",
        help="Comma-separated column names (default: *)"
    )

    parser.add_argument(
        "--where",
        default=None,
        help="SQL WHERE clause (without 'WHERE')"
    )

    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Limit number of rows"
    )

    parser.add_argument(
        "--out",
        required=True,
        help="Output CSV file path"
    )

    return parser.parse_args()


def build_query(args):
    query = f"SELECT {args.columns} FROM {args.table}"

    if args.where:
        query += f" WHERE {args.where}"

    if args.limit is not None:
        query += f" LIMIT {args.limit}"

    return query + ";"


def main():
    args = parse_args()

    if not os.path.exists(args.db):
        print(f"Database file not found: {args.db}", file=sys.stderr)
        sys.exit(1)

    query = build_query(args)

    conn = sqlite3.connect(args.db)
    cursor = conn.cursor()

    try:
        cursor.execute(query)
    except sqlite3.Error as e:
        print(f"SQL error: {e}", file=sys.stderr)
        conn.close()
        sys.exit(1)

    rows = cursor.fetchall()
    headers = [desc[0] for desc in cursor.description]

    with open(args.out, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        writer.writerows(rows)

    conn.close()

    print(f"Exported {len(rows)} rows to {args.out}")


if __name__ == "__main__":
    main()
