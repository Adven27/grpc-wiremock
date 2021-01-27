FROM adven27/grpc-wiremock
# copy our own proto files
COPY ./proto/*.proto /proto/
# Start image faster by compiling everything when we build the image
RUN gradle compileJava

# copy wiremock mappings and files - could also be mounted
COPY ./wiremock/mappings/* /wiremock/mappings/
COPY ./wiremock/__files/* /wiremock/__files/