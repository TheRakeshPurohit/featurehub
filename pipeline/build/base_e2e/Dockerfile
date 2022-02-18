FROM node:16

# we need Dart for the e2e tests
RUN apt-get update && apt-get install -y apt-transport-https curl
RUN sh -c 'wget -qO- https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -'
RUN sh -c 'wget -qO- https://storage.googleapis.com/download.dartlang.org/linux/debian/dart_stable.list > /etc/apt/sources.list.d/dart_stable.list'
RUN apt-get update && apt-get install dart

# and pulumi
RUN curl -fsSL https://get.pulumi.com | sh

