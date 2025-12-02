FROM eclipse-temurin:17-jdk-jammy

ENV ANDROID_SDK_ROOT /opt/android-sdk
RUN mkdir -p ${ANDROID_SDK_ROOT}

# Instalar dependências básicas
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# instalar command-line tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    cd /tmp && \
    curl -fSL "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip" -o /tmp/cmdline.zip && \
    unzip /tmp/cmdline.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline.zip

# criar usuário não-root (tenta usar USER_ID, cai para USER_ID+1 se já existir)
ARG USER_ID=1000
RUN if id -u ${USER_ID} >/dev/null 2>&1; then \
      useradd -m -u $((USER_ID+1)) developer; \
    else \
      useradd -m -u ${USER_ID} developer; \
    fi && \
    mkdir -p /home/developer/.android && \
    touch /home/developer/.android/repositories.cfg && \
    chown -R developer:developer ${ANDROID_SDK_ROOT} /home/developer/.android

USER developer
WORKDIR /workspace

ARG ANDROID_SDK_VERSION=33
ARG BUILD_TOOLS_VERSION=33.0.2

# Correção: determina JAVA_HOME a partir do java em PATH antes de chamar sdkmanager
RUN export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))" && \
    echo "Using JAVA_HOME=${JAVA_HOME}" && \
    yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses

RUN export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))" && \
    echo "Using JAVA_HOME=${JAVA_HOME}" && \
    ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} \
    "platform-tools" \
    "platforms;android-${ANDROID_SDK_VERSION}" \
    "build-tools;${BUILD_TOOLS_VERSION}"

CMD ["bash"]