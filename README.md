# Overview
grpc-wiremock is a **mock server** for **GRPC** services implemented as a wrapper around the [WireMock](http://wiremock.org) http server.

## How It Works
grpc-wiremock starts a grpc server generated based on provided proto files which will convert a proto grpc request to JSON and redirects it as a POST request to the WireMock then converts a http response back to grpc proto format.
1. GRPC server works on `tcp://localhost:50000`
2. WireMock server works on `http://localhost:8888`

## Quick Usage
- Run `docker run -p 8888:8888 -p 50000:50000 -v $(pwd)/example:/proto grpc-wiremock`
- Stub 
```
curl -X POST http://localhost:8888/__admin/mappings \
  -d '{
    "request": {
        "method": "POST",
        "url": "/",
        "bodyPatterns" : [ {
            "equalToJson" : { "greeting": "World" }
        } ]
    },
    "response": {
        "status": 200,
        "jsonBody": { 
            "reply": "Hello World", 
            "number": [1, 2] 
        }
    }
}'
```

- Check `grpcurl -plaintext -d '{"greeting": "World"}' localhost:50000 hello.HelloService/sayHello`

Response:
```
{
  "reply": "Hello World",
  "number": [
    1,
    2
  ]
}
```
## Stubbing

Stubbing should be done via [WireMock JSON API](http://wiremock.org/docs/stubbing/) 