## Configure the shell environment for dev
```bash
. ./dev-vars
```

## Configure the shell environment for prod
```bash
. ./prod-vars
```

## Configure the shell environment for console app
```bash
. ./console-vars
```

## Build the container environment
```bash
docker compose --profile development build
```

## Start the container environment
```bash
docker compose --profile development up --detach
```

## Start the container environment
```bash
docker compose --profile development down --volumes
```

## Start only the DB container environment
```bash
docker compose --file docker-compose.db --profile development up --detach
```

## Stop only the DB container environment
```bash
docker compose --file docker-compose.db --profile development down --volumes
```

