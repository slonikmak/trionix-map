#!/bin/bash
# Скрипт для запуска примеров Trionix Maps

echo "Trionix Maps - Примеры использования"
echo "======================================"
echo ""
echo "Запуск продвинутого примера (AdvancedMapExample)..."
# Build required modules and run the AdvancedMapExample using the JavaFX plugin.
# Use the run-demo profile which sets a sensible default main class so you can run with:
#   mvn -pl trionix-map-demo -am -Prun-demo javafx:run
# Ensure the core library is available in the local repo so the demo has the runtime dependency
mvn -pl trionix-map-core -am -DskipTests=true install

# Run the demo via the demo module's POM and the `run-demo` profile which configures javafx-maven-plugin
mvn -f trionix-map-demo/pom.xml -Prun-demo javafx:run
