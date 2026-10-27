#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
source ./.env.local
exec mvn spring-boot:run
