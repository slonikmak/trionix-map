# PowerShell скрипт для запуска примеров Trionix Maps

Write-Host "Trionix Maps - Примеры использования" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Выберите пример для запуска:"
Write-Host "1. Простейший пример (SimpleMapExample) - всего 20 строк кода"
Write-Host "2. Базовый пример (MapViewSampleApp) - карта с маркерами"
Write-Host "3. Продвинутый пример (AdvancedMapExample) - полнофункциональное приложение"
Write-Host ""

$choice = Read-Host "Введите номер (1, 2 или 3)"

switch ($choice) {
    "1" {
        Write-Host "Запуск простейшего примера..." -ForegroundColor Green
        mvn compile exec:java "-Dexec.mainClass=com.trionix.maps.samples.SimpleMapExample"
    }
    "2" {
        Write-Host "Запуск базового примера..." -ForegroundColor Green
        mvn compile exec:java "-Dexec.mainClass=com.trionix.maps.samples.MapViewSampleApp"
    }
    "3" {
        Write-Host "Запуск продвинутого примера..." -ForegroundColor Green
        mvn compile exec:java "-Dexec.mainClass=com.trionix.maps.samples.AdvancedMapExample"
    }
    default {
        Write-Host "Неверный выбор. Пожалуйста, введите 1, 2 или 3." -ForegroundColor Red
        exit 1
    }
}
