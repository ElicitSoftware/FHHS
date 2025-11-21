echo "Set java to 21"
jenv local 21.0.2
echo "build fhhs"

./mvnw clean package -Dmaven.test.skip=true -Dquarkus.profile=docker