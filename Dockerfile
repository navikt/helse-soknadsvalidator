FROM navikt/java:10

ENV APP_BINARY=spleis
COPY build/install/spleis/ .
