[![Stability: Maintenance](https://masterminds.github.io/stability/maintenance.svg)](https://masterminds.github.io/stability/maintenance.html)
[![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/adven27/grpc-wiremock?label=build&logo=docker)](https://hub.docker.com/repository/docker/adven27/grpc-wiremock/builds)
[![Docker Image Version (tag latest semver)](https://img.shields.io/docker/v/adven27/grpc-wiremock/1.0.0?logo=docker)](https://hub.docker.com/repository/docker/adven27/grpc-wiremock/general)

# Overview
grpc-wiremock is a **mock server** for **GRPC** services implemented as a wrapper around the [WireMock](http://wiremock.org) http server.

## How It Works
grpc-wiremock starts a grpc server generated based on provided proto files which will convert a proto grpc request to JSON and redirects it as a POST request to the WireMock then converts a http response back to grpc proto format.
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
        "bodyPatterns" : [ {
            "equalToJson" : { "id": "1", "currency": "EUR" }
        } ]
    },
    "response": {
        "status": 200,
        "jsonBody": { 
            "balance": { 
                "amount": { "value": { "decimal" : "100.0" }, "value_present": true },
                "currency": { "value": "EUR", "value_present": true }
            } 
        }
    }
}'
```

3) Check 
```posh
grpcurl -plaintext -d '{"id": 1, "currency": "EUR"}' localhost:50000 api.wallet.BalanceService/getUserBalance
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

## How To:

#### 1. Change grpc server properties

Currently, following grpc server properties are supported<sup>*</sup>:

```properties
GRPC_SERVER_MAXHEADERLISTSIZE
GRPC_SERVER_MAXMESSAGESIZE
GRPC_SERVER_MAXINBOUNDMETADATASIZE
GRPC_SERVER_MAXINBOUNDMESSAGESIZE
```
<sub>*The first two are deprecated in favor of the last two</sub>

Could be used like this:

```posh
docker run -e GRPC_SERVER_MAXHEADERLISTSIZE=1000 adven27/grpc-wiremock
```

#### 2. Speed up container start

In case you don't need to change proto files, you can build your own image with precompiled protos.  
See an [example](/example/Dockerfile)

#### 3. Use with snappy compresser/decompresser

Snappy support can be enabled using EXT_CODECS env variable as follows:
```posh
docker run -e EXT_CODECS=snappy adven27/grpc-wiremock
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
      - EXT_CODECS=snappy
```
<sub>*gzip compression supported by default</sub>


#### 4. Use in load testing

To increase performance some Wiremock related options may be tuned either directly or by enabling the "load" profile. 
Next two commands are identical:
```posh
docker run -e SPRING_PROFILES_ACTIVE=load adven27/grpc-wiremock
```
```posh
docker run \
    -e WIREMOCK_SERVER_DISABLEREQUESTJOURNAL=true \
    -e WIREMOCK_SERVER_ASYNCHRONOUSRESPONSEENABLED=true \
    -e WIREMOCK_SERVER_ASYNCHRONOUSRESPONSETHREADS=10 \
    -e WIREMOCK_SERVER_STUBREQUESTLOGGINGDISABLED=true \
    -e WIREMOCK_SERVER_VERBOSE=false \
    adven27/grpc-wiremock
```
