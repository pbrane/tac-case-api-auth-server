#!/bin/bash

az containerapp create \
  --name authdb-prod \
  --resource-group MolexProject \
  --environment tac-case-api-env-prod \
  --env-vars POSTGRES_USER="tacauthuser" \
             POSTGRES_PASSWORD="tacauthpass" \
             POSTGRES_DB="tacauthdb" \
             PGDATA="/authdb-prod/data/postgres" \
  --image postgres:17 \
  --ingress internal \
  --allow-insecure true \
  --target-port 5432 \
  --min-replicas 1 \
  --max-replicas 3
