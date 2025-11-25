#!/bin/bash
# Скрипт для запуска примеров Trionix Maps

echo "Trionix Maps - Примеры использования"
echo "======================================"
echo ""
echo "Выберите пример для запуска:"
echo "1. Простейший пример (SimpleMapExample) - всего 20 строк кода"
echo "2. Базовый пример (MapViewSampleApp) - карта с маркерами"
echo "3. Продвинутый пример (AdvancedMapExample) - полнофункциональное приложение"
echo ""
read -p "Введите номер (1, 2 или 3): " choice

case $choice in
    1)
        echo "Запуск простейшего примера..."
        mvn -pl trionix-map-core,trionix-map-demo compile
        mvn -f trionix-map-demo/pom.xml exec:java -Dexec.mainClass="com.trionix.maps.samples.SimpleMapExample"
        ;;
    2)
        echo "Запуск базового примера..."
        mvn -pl trionix-map-core,trionix-map-demo compile
        mvn -f trionix-map-demo/pom.xml exec:java -Dexec.mainClass="com.trionix.maps.samples.MapViewSampleApp"
        ;;
    3)
        echo "Запуск продвинутого примера..."
        mvn -pl trionix-map-core,trionix-map-demo compile
        mvn -f trionix-map-demo/pom.xml exec:java -Dexec.mainClass="com.trionix.maps.samples.AdvancedMapExample"
        ;;
    *)
        echo "Неверный выбор. Пожалуйста, введите 1, 2 или 3."
        exit 1
        ;;
esac
