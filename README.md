# implementation-sample-api-java

This is a SaaS implementation sample using the SaaSus SDK

See the documentation [API implementation using SaaS Platform](https://docs.saasus.io/ja/docs/implementation-guide/implementing-authentication-using-saasus-platform-apiserver)

## Requirements
- docker
- maven (latest)

## Setup SDK
```sh
# Run in any directory
git clone git@github.com:saasus-platform/saasus-sdk-java.git
cd saasus-sdk-java
mvn clean package
mvn install:install-file -Dfile=target/saasus-sdk-java-1.0.0.jar -DgroupId=io.saasus -DartifactId=saasus-java -Dversion=0.0.1 -Dpackaging=jar
```

## Run Java API
```sh
# Run in any directory
git clone git@github.com:saasus-platform/implementation-sample-api-java
cd implementation-sample-api-java
mvn clean package

cp .env.example .env
vi .env

# Set Env for SaaSus Platform API
# Get it in the SaaSus Admin Console
SAASUS_SAAS_ID="xxxxxxxxxx"
SAASUS_API_KEY="xxxxxxxxxx"
SAASUS_SECRET_KEY="xxxxxxxxxx"

# Save and exit
```

```
docker compose up -d
```
