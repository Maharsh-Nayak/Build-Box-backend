FROM node:20-alpine

RUN apk add --no-cache \
    git \
    python3 \
    make \
    g++ \
    aws-cli \
    docker-cli \
    redis

WORKDIR /app

COPY build.sh /build.sh
RUN chmod +x /build.sh && sed -i 's/\r$//' /build.sh

ENTRYPOINT ["/build.sh"]