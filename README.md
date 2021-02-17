[![](https://images.microbadger.com/badges/version/javiyt/grpc-wiremock.svg)](https://microbadger.com/images/javiyt/grpc-wiremock "Get your own version badge on microbadger.com")
[![](https://images.microbadger.com/badges/image/javiyt/grpc-wiremock.svg)](https://microbadger.com/images/javiyt/grpc-wiremock "Get your own image badge on microbadger.com")

# Overview
grpc-wiremock is a **mock server** for **GRPC** services implemented as a wrapper around the [WireMock](http://wiremock.org) http server.

## How It Works
grpc-wiremock starts a grpc server generated based on provided proto files which will convert a proto grpc request to JSON and redirects it as a POST request to the WireMock then converts a http response back to grpc proto format.
1. GRPC server works on `tcp://localhost:50000`
2. WireMock server works on `http://localhost:8888`

## Quick Usage
1) Run 
```posh
docker run -p 8888:8888 -p 50000:50000 -v $(pwd)/example/proto:/proto -v $(pwd)/example/wiremock:/wiremock javiyt/grpc-wiremock
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

1. Change grpc server properties

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

2. Speed up container start

In case you don't need to change proto files, you can build your own image with precompiled protos.  
See an [example](/example/Dockerfile)

3. Use in load testing

To increase performance some Wiremock related options may be tuned either directly or by enabling the "load" profile. 
Next to commands are identical:
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

## Error codes
When testing errors from the server side you can set up your stubs taking into account the following equivalences table:

| HTTP Status Code           | gRPC Status Code   |
|----------------------------|--------------------|
| 400 Bad Request            | INTERNAL           |
| 401 Unauthorized           | UNAUTHENTICATED    |
| 403 Forbidden              | PERMISSION\_DENIED |
| 404 Not Found              | UNIMPLEMENTED      |
| 429 Too Many Requests      | UNAVAILABLE        |
| 502 Bad Gateway            | UNAVAILABLE        |
| 503 Service Unavailable    | UNAVAILABLE        |
| 504 Gateway Timeout        | UNAVAILABLE        |
| _All other codes_          | UNKNOWN            |

Extracted from [HTTP to gRPC Status Code Mapping](https://github.com/grpc/grpc/edit/master/doc/http-grpc-status-mapping.md)

Example stub:
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
        "status": 403,
        "body": "you can't access here"
    }
}'
```

The response you will get when running the gRPC query is:
```json
{
"error": "7 PERMISSION_DENIED: "
}
```