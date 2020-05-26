[![](https://images.microbadger.com/badges/version/adven27/grpc-wiremock.svg)](https://microbadger.com/images/adven27/grpc-wiremock "Get your own version badge on microbadger.com")
[![](https://images.microbadger.com/badges/image/adven27/grpc-wiremock.svg)](https://microbadger.com/images/adven27/grpc-wiremock "Get your own image badge on microbadger.com")

# Overview
grpc-wiremock is a **mock server** for **GRPC** services implemented as a wrapper around the [WireMock](http://wiremock.org) http server.

## How It Works
grpc-wiremock starts a grpc server generated based on provided proto files which will convert a proto grpc request to JSON and redirects it as a POST request to the WireMock then converts a http response back to grpc proto format.
1. GRPC server works on `tcp://localhost:50000`
2. WireMock server works on `http://localhost:8888`

## Quick Usage
1) Run 
```posh
docker run -p 8888:8888 -p 50000:50000 -v $(pwd)/example:/proto adven27/grpc-wiremock
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
