echo "Set java to 25"
jenv local graalvm64-25.0.4
export JAVA_HOME="$(jenv javahome)"
echo "build fhhs"

./mvnw clean package -Dquarkus.profile=docker