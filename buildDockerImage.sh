echo "Set java to 25"
jenv local 25
echo "build fhhs"

./mvnw clean package -Dmaven.test.skip=true -Dquarkus.profile=docker