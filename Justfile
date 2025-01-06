# Strict mode
set shell := ["bash", "-euo", "pipefail", "-c"]

# Globals
# export GIT_SHA := `git rev-parse --short HEAD`

[private]
default:
	@just --list --unsorted

build:
    ./gradlew clean build shadowJar

package:
    docker build .

migrate:
    liquibase --defaultsFile=database/liquibase.properties update

run:
    java -jar build/libs/pulse-sink-0.1-all.jar

submit sink:
    curl -X POST "http://localhost:8080/v1/sink/submit/{{sink}}" \
        -H "Content-Type: application/json" \
        -d '[{"key1":"value1","key2":42,"key3":true},{"key1":"value2","key2":123,"key3":false}]' | jq .

benchmark number:
     ab -n {{number}} -c 10 -p benchmark/data.json -T 'application/json' http://localhost:8080/v1/sink/submit/pay-api-trace-schema


submit-trace:
    curl -X POST "http://localhost:8080/v1/sink/submit/pay-api-trace-schema" \
    -H "Content-Type: application/json" \
    -d '[{"api_cl_log_id":"123e4567-e89b-12d3-a456-426614174000","instance_id":1,"ws_id":42,"client_id":"223e4567-e89b-12d3-a456-426614174001","api_call_unique_identifier":"323e4567-e89b-12d3-a456-426614174002","cre_dt":"2024-12-31T12:34:56Z","log_level":"INFO","api_name":"ExampleAPI","msg_detail":"This is a detailed log message about the API call."}]'
