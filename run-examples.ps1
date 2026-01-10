# PowerShell скрипт для запуска примеров Trionix Maps

Write-Host "Trionix Maps - Примеры использования" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Запуск продвинутого примера (AdvancedMapExample)..." -ForegroundColor Green
# Build required modules and run the AdvancedMapExample using the JavaFX maven plugin.
# Uses -pl with -am so the core module is built into the reactor classpath.
# The demo module has a convenience profile (`run-demo`) which sets the default main class.
# This avoids shell quoting issues with -Djavafx.mainClass on some platforms.
# Ensure the core library is available on the local repo/classpath for the demo run
# mvn -pl trionix-map-core -am -DskipTests=true install

# Now run the demo using the JavaFX plugin configured in the demo module's run-demo profile
mvn -f trionix-map-demo/pom.xml -Prun-demo javafx:run
