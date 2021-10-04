[![Stability: Maintenance](https://masterminds.github.io/stability/maintenance.svg)](https://masterminds.github.io/stability/maintenance.html)
[![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/adven27/grpc-wiremock?label=build&logo=docker)](https://hub.docker.com/repository/docker/adven27/grpc-wiremock/builds)
[![Docker Image Version (tag latest semver)](https://img.shields.io/docker/v/adven27/grpc-wiremock/1.2.1?logo=docker)](https://hub.docker.com/repository/docker/adven27/grpc-wiremock/general)

# Overview
grpc-wiremock is a **mock server** for **GRPC** services implemented as a wrapper around the [WireMock](http://wiremock.org) http server.

## How It Works
[![Overview](doc/overview.png)]()
*grpc-wiremock* starts a gRPC server generated based on provided proto files which will convert a proto grpc request to JSON and redirects it as a POST request to the WireMock then converts a http response back to grpc proto format.
1. GRPC server works on `tcp://localhost:50000`
2. WireMock server works on `http://localhost:8888`

## Quick Usage
1) Run 
```posh
docker run -p 8888:8888 -p 50000:50000 -v $(pwd)/example/proto:/proto -v $(pwd)/example/wiremock:/wiremock adven27/grpc-wiremock
```

2) Stub 
```json
curl -X POST http://localhost:8888/__admin/mappings \
  -d '{
    "request": {
        "method": "POST",
        "url": "/BalanceService/getUserBalance",
        "headers": {"withAmount": {"matches": "\\d+\\.?\\d*"} },
        "bodyPatterns" : [ {
            "equalToJson" : { "id": "1", "currency": "EUR" }
        } ]
    },
    "response": {
        "status": 200,
        "jsonBody": { 
            "balance": { 
                "amount": { "value": { "decimal" : "{{request.headers.withAmount}}" }, "value_present": true },
                "currency": { "value": "EUR", "value_present": true }
            } 
        }
    }
}'
```

3) Check 
```json
grpcurl -H 'withAmount: 100.0' -plaintext -d '{"id": 1, "currency": "EUR"}' localhost:50000 api.wallet.BalanceService/getUserBalance
```

Should get response:
```json
{
  "balance": {
    "amount": {
      "value": {
        "decimal": "100.0"
      },
      "value_present": true
    },
    "currency": {
      "value": "EUR",
      "value_present": true
    }
  }
}
```
## Stubbing

Stubbing should be done via [WireMock JSON API](http://wiremock.org/docs/stubbing/) 

### Error mapping

Default error (not `200 OK`) mapping is based on https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto :

| HTTP Status Code         | GRPC Status       | 
| ------------------------ |:-----------------:|
| 400 Bad Request          | INVALID_ARGUMENT  |
| 401 Unauthorized         | UNAUTHENTICATED   |
| 403 Forbidden            | PERMISSION_DENIED |
| 404 Not Found            | NOT_FOUND         |
| 409 Conflict             | ALREADY_EXISTS    |
| 429 Too Many Requests    | RESOURCE_EXHAUSTED|
| 499 Client Closed Request| CANCELLED         |
| 500 Internal Server Error| INTERNAL          |
| 501 Not Implemented      | UNIMPLEMENTED     |
| 503 Service Unavailable  | UNAVAILABLE       |
| 504 Gateway Timeout      | DEADLINE_EXCEEDED |

And could be overridden or augmented by overriding or augmenting the following properties:
```yaml
grpc:
  error-code-by:
    http:
      status-code:
        400: INVALID_ARGUMENT
        401: UNAUTHENTICATED
        403: PERMISSION_DENIED
        404: NOT_FOUND
        409: ALREADY_EXISTS
        429: RESOURCE_EXHAUSTED
        499: CANCELLED
        500: INTERNAL
        501: UNIMPLEMENTED
        503: UNAVAILABLE
        504: DEADLINE_EXCEEDED
```
For example:
```posh
docker run \
    -e GRPC_ERRORCODEBY_HTTP_STATUSCODE_400=OUT_OF_RANGE \
    -e GRPC_ERRORCODEBY_HTTP_STATUSCODE_510=DATA_LOSS \
    adven27/grpc-wiremock
```
## How To:

### 1. Configuring gRPC server

Currently, following grpc server properties are supported:

```properties
GRPC_SERVER_PORT
GRPC_SERVER_MAXHEADERLISTSIZE
GRPC_SERVER_MAXMESSAGESIZE
GRPC_SERVER_MAXINBOUNDMETADATASIZE
GRPC_SERVER_MAXINBOUNDMESSAGESIZE
```

Could be used like this:

```posh
docker run -e GRPC_SERVER_MAXHEADERLISTSIZE=1000 adven27/grpc-wiremock
```

### 2. Configuring WireMock server

WireMock server may be configured by passing [command line options](http://wiremock.org/docs/running-standalone/) 
prefixed by `wiremock_`:

```posh
docker run -e WIREMOCK_DISABLE-REQUEST-LOGGING -e WIREMOCK_PORT=0 adven27/grpc-wiremock
```

### 3. Speed up container start

In case you don't need to change proto files, you can build your own image with precompiled protos.  
See an [example](/example/Dockerfile)

### 4. Use with snappy compresser/decompresser

Snappy support can be enabled using `EXTERNAL_CODECS` env variable as follows:
```posh
docker run -e EXTERNAL_CODECS="snappy, another" adven27/grpc-wiremock
```
Also in docker-compose:
```posh
    image: adven27/grpc-wiremock
    ports:
      - "12085:50000" # grpc port
      - "8088:8888" # http serve port
    volumes:
      - ./example/proto:/proto
    environment:
      - EXTERNAL_CODECS=snappy
```
<sub>*gzip compression supported by default</sub>


### 5. Use in load testing

To increase performance some Wiremock related options may be tuned either directly or by enabling the "load" profile. 
Next two commands are identical:
```posh
docker run -e SPRING_PROFILES_ACTIVE=load adven27/grpc-wiremock
```
```posh
docker run \
  -e WIREMOCK_NO-REQUEST-JOURNAL \
  -e WIREMOCK_DISABLE-REQUEST-LOGGING \
  -e WIREMOCK_ASYNC-RESPONSE-ENABLED \
  -e WIREMOCK_ASYNC-RESPONSE-THREADS=10 \
  adven27/grpc-wiremock
```
