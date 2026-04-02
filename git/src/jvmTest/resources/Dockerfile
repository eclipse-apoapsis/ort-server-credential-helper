FROM alpine:3.21

RUN apk add --no-cache \
    git \
    git-daemon \
    nginx \
    fcgiwrap \
    apache2-utils \
    spawn-fcgi \
    gcompat \
    libgcc

COPY entrypoint.sh /entrypoint.sh
COPY nginx.conf /etc/nginx/nginx.conf

RUN chmod +x /entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
