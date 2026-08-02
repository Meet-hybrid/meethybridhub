#!/usr/bin/env bash
# ============================================================================
# Local PostgreSQL (no root) — development helper for MeethybridHub.
#
# Why this exists: PostgreSQL 16 was installed WITHOUT root by extracting the
# Ubuntu noble .debs into ~/pgroot (see README, "Local PostgreSQL without
# root"). That bypasses the usual postinst setup, so there is no systemd
# service and no system-wide library path — this script manages a single-user
# cluster running as $USER instead.
#
#   ./scripts/db.sh start   # initdb (first run) + start on :5432
#   ./scripts/db.sh stop    # graceful shutdown
#   ./scripts/db.sh status  # is it up?
#   ./scripts/db.sh psql    # interactive psql as postgres
# ============================================================================
set -euo pipefail

PGROOT="$HOME/pgroot"
PGBIN="$PGROOT/usr/lib/postgresql/16/bin"
PGDATA="$HOME/pgdata"
PGLOG="$PGDATA/server.log"
export LD_LIBRARY_PATH="$PGROOT/usr/lib/x86_64-linux-gnu:${LD_LIBRARY_PATH:-}"

PORT=5432
DB_NAME="meethybridhub"
SUPERUSER="postgres"
SUPERUSER_PASSWORD="postgres"   # dev-only credential, matches application.yml defaults

usage() { echo "usage: $0 {start|stop|status|psql}"; exit 1; }
[ "$#" -ge 1 ] || usage

case "$1" in
  start)
    if [ ! -d "$PGDATA" ]; then
      echo "==> Initializing cluster at $PGDATA (superuser: $SUPERUSER)"
      echo "$SUPERUSER_PASSWORD" > /tmp/.pgpw
      "$PGBIN/initdb" -D "$PGDATA" -U "$SUPERUSER" --pwfile=/tmp/.pgpw -A scram-sha-256
      rm -f /tmp/.pgpw
    fi
    # Idempotent: starting an already-running cluster must not fail (pg_ctl start
    # exits non-zero if the server is already up, which under `set -e` would abort).
    if "$PGBIN/pg_ctl" -D "$PGDATA" status >/dev/null 2>&1; then
      echo "==> PostgreSQL already running (port $PORT)"
    else
      "$PGBIN/pg_ctl" -D "$PGDATA" -l "$PGLOG" -o "-k /tmp -p $PORT" start
    fi
    # Idempotently create the application database (the app's DataSource target).
    if ! PGPASSWORD="$SUPERUSER_PASSWORD" "$PGBIN/psql" -h localhost -p "$PORT" -U "$SUPERUSER" -lqt \
        | cut -d'|' -f1 | grep -qw "$DB_NAME"; then
      PGPASSWORD="$SUPERUSER_PASSWORD" "$PGBIN/createdb" -h localhost -p "$PORT" -U "$SUPERUSER" "$DB_NAME"
      echo "==> Created database: $DB_NAME"
    fi
    ;;
  stop)
    "$PGBIN/pg_ctl" -D "$PGDATA" stop || echo "==> PostgreSQL is not running"
    ;;
  status)
    "$PGBIN/pg_ctl" -D "$PGDATA" status || echo "PostgreSQL is not running (use ./scripts/db.sh start)"
    ;;
  psql)
    PGPASSWORD="$SUPERUSER_PASSWORD" "$PGBIN/psql" -h localhost -p "$PORT" -U "$SUPERUSER" "${@:2}"
    ;;
  *)
    usage
    ;;
esac
