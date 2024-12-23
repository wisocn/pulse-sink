# Strict mode
set shell := ["bash", "-euo", "pipefail", "-c"]

# Globals
# export GIT_SHA := `git rev-parse --short HEAD`

[private]
default:
	@just --list --unsorted

build:
    ./gradlew clean build shadowJar

run:
    java -jar build/libs/pulse-sink-0.1-all.jar

