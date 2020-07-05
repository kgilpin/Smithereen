FROM maven:3-jdk-11 as builder

# Build JAR

WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn package

FROM openjdk:11

# Install dependencies

SHELL ["bash", "-c"]

RUN apt-get update
RUN apt-get install -y --no-install-recommends libglib2.0-0 libpng16-16 libjpeg62-turbo libgif7 libwebp6 libexpat1

RUN mkdir -p /opt/smithereen
WORKDIR /opt/smithereen
COPY --from=builder /usr/src/app/target/smithereen-jar-with-dependencies.jar /opt/smithereen/smithereen.jar

# Copy all JNI libraries into the container, then move one matching the CPU architecture, then delete the rest

COPY jniPrebuilt jniPrebuilt
RUN ARCH= && \
    dpkgArch="$(dpkg --print-architecture)" && \
  case "${dpkgArch##*-}" in \
    amd64) ARCH='x86_64';; \
    arm64) ARCH='arm64';; \
    armhf) ARCH='armv7';; \
    *) echo "unsupported architecture"; exit 1 ;; \
  esac && \
  mv jniPrebuilt/linux-$ARCH/libvips_jni.so libvips_jni.so && \
  rm -rf jniPrebuilt

EXPOSE 4567
ENTRYPOINT java -jar /opt/smithereen/smithereen.jar /usr/local/etc/config.properties