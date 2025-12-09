# Guia de Uso com Docker

Este projeto já está configurado para rodar com Docker. Abaixo estão os passos para construir e gerar o APK.

## Pré-requisitos

- Docker e Docker Compose instalados.

## Passos

1.  **Construir a imagem Docker:**
    Abra o terminal na pasta do projeto e execute:
    ```bash
    docker-compose build
    ```

2.  **Iniciar o container:**
    Para entrar no ambiente de desenvolvimento dentro do container:
    ```bash
    docker-compose run --rm android-build bash
    ```

3.  **Gerar o APK (dentro do container):**
    Uma vez dentro do terminal do container, execute o comando do Gradle para montar o APK de debug:
    ```bash
    sed -i 's/\r$//' gradlew 
    chmod +x gradlew
    ./gradlew assembleDebug
    ```
    
    *Nota: Na primeira execução, o Gradle irá baixar todas as dependências. Isso pode levar de 5 a 10 minutos e o terminal pode parecer "travado" sem mostrar nada. Tenha paciência.*

    *Nota:
    "sed -i 's/\r$//' gradlew" corrige quebras de linhas do windows para linux.
    "chmod +x gradlew" adiciona permissão de execução ao arquivo gradlew.
    "./gradlew assembleDebug" executa o gradle para montar o apk.
    
    *Dica: Se quiser ver o que está acontecendo, use o comando com `-i`:*
    ```bash
    ./gradlew assembleDebug -i
    ```
    
4.  **Localizar o APK:**
    Após o término do build, o APK gerado estará disponível na sua máquina local (fora do docker) em:
    `app/build/outputs/apk/debug/app-debug.apk`

## Comandos Úteis

- **Limpar o projeto:**
  ```bash
  ./gradlew clean
  ```

- **Verificar tarefas disponíveis:**
  ```bash
  ./gradlew tasks
  ```

## Solução de Problemas

- **Permissão Negada no gradlew:**
  Se ao tentar rodar `./gradlew` você receber um erro de permissão, execute:
  ```bash
  chmod +x gradlew
  ```

- **Erro de conexão (error during connect):**
  Se aparecer um erro como `open //./pipe/dockerDesktopLinuxEngine: O sistema não pode encontrar o arquivo especificado`, significa que o **Docker Desktop não está rodando**.
  1. Abra o "Docker Desktop" no Windows.
  2. Aguarde o ícone da baleia ficar verde (ou parar de animar).
  3. Tente o comando novamente.

- **Erro `sh\r`: No such file or directory:**
  Isso acontece porque o arquivo `gradlew` está com quebras de linha do Windows (CRLF) em vez de Linux (LF).
  Para corrigir, rode este comando **dentro do container**:
  ```bash
  sed -i 's/\r$//' gradlew
  ```
  E tente rodar o `./gradlew assembleDebug` novamente.
