# Image for Cesium releases on Linux.
#
#
# Building this image:
#   docker build . --network=host -t duniter/duniter4j-es
#
# Test:
#   docker run --net=host -t duniter/duniter4j-es
# Test (interactive mode + bash mode):
#   docker run -i --net=host -t duniter/duniter4j-es bash
#
# Pull base image.
FROM airdock/oracle-jdk:1.8

ARG DUNITER4J_VERSION=1.0.2
ARG LIBSODIUM_VERSION=1.0.13

# Installing dependencies
RUN apt-get update && \
  apt-get --force-yes --yes install wget unzip build-essential

# Installing libsodium
RUN wget https://download.libsodium.org/libsodium/releases/libsodium-${LIBSODIUM_VERSION}.tar.gz && \
    tar -xzf libsodium-*.tar.gz && rm *.tar.gz && mv libsodium-* libsodium && \
    cd libsodium  && \
    ./configure && \
    make && make check && \
    make install

# Create compiling user
RUN mkdir /duniter4j && \
	adduser --system --group --quiet --shell /bin/bash --home /duniter4j duniter4j && \
	chown duniter4j:duniter4j /duniter4j
WORKDIR /duniter4j

#RUN cd /duniter4j && \
#	wget https://git.duniter.org/clients/cesium-grp/cesium/repository/v${CESIUM_VERSION}/archive.tar.gz
#   tar -xzf archive.tar.gz && rm *.tar.gz && mv cesium-* src && \

RUN cd /duniter4j && \
    wget https://github.com/duniter/duniter4j/releases/download/duniter4j-${DUNITER4J_VERSION}/duniter4j-client-${DUNITER4J_VERSION}-standalone.zip && \
    unzip *.zip && rm *.zip && mv duniter4j-client-* duniter4j-client && \
    mkdir duniter4j-es/data && \
    chown -R duniter4j:duniter4j duniter4j-client

RUN ln -s /duniter4j/duniter4j-client/bin/elasticsearch /usr/bin/duniter4j-client

VOLUME /duniter4j/duniter4j-client
EXPOSE 9200 9400

USER duniter4j
WORKDIR /duniter4j

ENTRYPOINT ["/usr/bin/duniter4j-client"]
CMD []
