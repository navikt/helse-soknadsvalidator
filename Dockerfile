FROM navikt/java:10

ENV APP_BINARY=sykepengesoknadvalidator
COPY build/install/sykepengesoknadvalidator/ .
