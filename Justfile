# Strict mode
set shell := ["bash", "-euo", "pipefail", "-c"]

# Globals
export GIT_SHA := `git rev-parse --short HEAD`
export POSTGRES_HOST := "localhost"
export POSTGRES_PORT := "5436"
export POSTGRES_USER := "user1"

[private]
default:
	@just --list --unsorted

run-docker:
    docker-compose up -d

build:
    ./gradlew clean build shadowJar

package: build
    docker build .

setup: run-docker
    #!/usr/local/bin/bash
    echo "Waiting for postgres"
    until pg_isready -h {{POSTGRES_HOST}} -p {{POSTGRES_PORT}} -U {{POSTGRES_USER}}; do
        echo "Postgres is not ready. Retrying in 2 seconds..."
        sleep 2
    done
    echo "PostgreSQL is ready. Running migrations..."
    liquibase --defaultsFile=database/liquibase.properties update

run:
    java -jar build/libs/pulse-sink-0.1-all.jar

teardown:
    docker-compose down

benchmark number:
     ab -n {{number}} -c 10 -p benchmark/data.json -T 'application/json' http://localhost:8080/v1/sink/submit/universal-analytics-trace-schema
