$jdk = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
$root = $PSScriptRoot

docker compose -f "$root\docker-compose.yml" up -d

$services = @("user-service", "complaint-service", "api-gateway")

foreach ($s in $services) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command",
        "`$env:JAVA_HOME='$jdk'; cd '$root\backend\$s'; .\mvnw spring-boot:run"
}