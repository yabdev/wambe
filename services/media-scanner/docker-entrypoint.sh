#!/bin/sh
set -eu

freshclam || true
clamd --config-file=/etc/clamav/clamd.conf &

attempt=0
until printf 'zPING\0' | nc -w 1 127.0.0.1 3310 2>/dev/null | grep -q PONG; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo "ClamAV failed to become ready" >&2
    exit 1
  fi
  sleep 1
done

exec su -s /bin/sh -c 'exec java -XX:MaxRAMPercentage=70 -jar /app/media-scanner.jar' wambe
