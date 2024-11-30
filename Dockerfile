FROM fedora:41
WORKDIR /app
COPY target/oda-media-service /app

CMD ["./oda-media-service"]
