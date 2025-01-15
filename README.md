## 📄 Table of Contents

- [📘 Overview](#-overview)
- [🚀 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [📦 Usage](#-usage)
  - [Setup](#setup)
  - [Building](#building)
  - [Running](#running)
  - [Load Benchmarking](#load-benchmarking)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## 📘 Overview

This repository hosts an example project designed to achieve a high-volume, low-latency analytics ingester while keeping things simple. The project sets up an API tied to PostgreSQL with PgAdmin for administration, migrates the database schema, and offers a JSON API (sink) to push JSON data to the ingester.

On my machine (M1 MacBook Pro), I achieved over 6k+ RPS throughput with a P99 latency of 8ms and a P(MAX) of 236ms:

```
Server Software:        
Server Hostname:        localhost
Server Port:            8080

Concurrency Level:      10
Time taken for tests:   1.284 seconds
Complete requests:      10000
Failed requests:        0
Total transferred:      750000 bytes
Total body sent:        6310000
HTML transferred:       0 bytes
Requests per second:    7790.46 [#/sec] (mean)
Time per request:       1.284 [ms] (mean)
Time per request:       0.128 [ms] (mean, across all concurrent requests)
Transfer rate:          570.59 [Kbytes/sec] received
                        4800.57 kb/s sent
                        5371.16 kb/s total

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   2.1      0      63
Processing:     0    1   1.8      1      47
Waiting:        0    1   1.6      1      47
Total:          0    1   3.4      1      90

Percentage of the requests served within a certain time (ms)
  50%      1
  66%      1
  75%      1
  80%      1
  90%      2
  95%      2
  98%      3
  99%      4
 100%     90 (longest request)

```

**Note:** This project uses PostgreSQL, an OLTP database. For analytics in a production system, consider using OLAP databases like BigQuery or Snowflake.

# 🚀 Getting Started

### Prerequisites

Before using any utilities from this repository, ensure you have the following installed:

- **Java 17 or later**
- **Gradle**
- **Git**
- **Docker/Docker Compose**
- **Just**

### Installation

You can use `.nix` and `flake.nix` to provision all required tools (except Git and Docker), or install the prerequisites via your package manager of choice.

## 📦 Usage

The entry point for this project is the **Justfile**. To see all available tasks, execute the command:

### Available Recipes

- `setup`
- `build`
- `package`
- `migrate`
- `run`
- `teardown`
- `benchmark number`



###  Setup
To setup everything we need for API to run we can use a just command `just setup`.
This will:
1. Spin up Docker containers (PostgreSQL, PgAdmin).
2. Wait until PostgreSQL is available and accepting connections.
3. Run Liquibase migrations from the `database` subfolder.

### Building

To build the project we execute `just build` which will trigger `./gradlew clean build shadowJar` and execute build and jar
packaging.

### Running 

Once the project has been built you can run it using `just run` which will spin a new java process with API.

### Load benchmarking

Benchmarking is supported using the Apache Benchmark utility (`ab`). 
To benchmark the API, use: `just benchmark {{number}}`. 
For example: 
1. `just benchmark 100`
2. `just benchmark 1000`
3. `just benchmark 10000`
etc..
