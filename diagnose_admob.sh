#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}   DIAGNÓSTICO COMPLETO ADMOB    ${NC}"
echo -e "${GREEN}==================================${NC}\n"

# Función para verificar archivos
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 existe"
        return 0
    else
        echo -e "${RED}✗${NC} $1 NO existe"
        return 1
    fi
}

# 1. Verificar estructura de archivos
echo -e "${YELLOW}1. VERIFICANDO ARCHIVOS CRÍTICOS:${NC}"
check_file "app/google-services.json"
check_file "app/src/main/java/com/follgramer/diamantesproplayersgo/ads/AdsInit.kt"
check_file "app/src/main/java/com/follgramer/diamantesproplayersgo/ads/AdIds.kt"

# 2. Verificar IDs de AdMob
echo -e "\n${YELLOW}2. IDS DE ADMOB CONFIGURADOS:${NC}"
echo "App ID en Manifest:"
grep -o "ca-app-pub-[0-9]*~[0-9]*" app/src/main/AndroidManifest.xml | uniq

echo -e "\nAd Unit IDs en código:"
grep -o "ca-app-pub-[0-9]*/[0-9]*" app/src/main -r --include="*.kt" | sort | uniq

# 3. Verificar modo DEBUG/RELEASE
echo -e "\n${YELLOW}3. CONFIGURACIÓN BUILD:${NC}"
grep "BuildConfig.DEBUG\|BuildConfig.USE_TEST_ADS" app/src/main -r --include="*.kt" | head -5

# 4. Buscar errores comunes
echo -e "\n${YELLOW}4. ERRORES COMUNES DETECTADOS:${NC}"

# Error: Inicialización después de carga
if grep -q "loadAd.*MobileAds.initialize\|loadAd.*before.*initialize" app/src/main -r --include="*.kt"; then
    echo -e "${RED}✗ Posible: loadAd() llamado antes de inicialización${NC}"
fi

# Error: No verificar consentimiento
if ! grep -q "canRequestAds()" app/src/main -r --include="*.kt"; then
    echo -e "${RED}✗ No se verifica canRequestAds() antes de cargar${NC}"
fi

# Error: IDs hardcodeados
if grep -q '"ca-app-pub-[0-9]*[~/][0-9]*"' app/src/main/java/com/follgramer/diamantesproplayersgo/MainActivity.kt; then
    echo -e "${YELLOW}⚠ IDs hardcodeados en MainActivity${NC}"
fi

# 5. Estado de dependencias
echo -e "\n${YELLOW}5. VERSIONES DE DEPENDENCIAS:${NC}"
grep "play-services-ads" app/build.gradle.kts
grep "user-messaging-platform" app/build.gradle.kts

# 6. Generar reporte
echo -e "\n${YELLOW}6. GENERANDO REPORTE...${NC}"
{
    echo "ADMOB DIAGNOSTIC REPORT - $(date)"
    echo "======================================"
    echo ""
    echo "Package: com.follgramer.diamantesproplayersgo"
    echo "AdMob App ID: $(grep -o 'ca-app-pub-[0-9]*~[0-9]*' app/src/main/AndroidManifest.xml | head -1)"
    echo ""
    echo "Build Config:"
    grep "versionName\|versionCode\|minSdk\|targetSdk" app/build.gradle.kts
    echo ""
    echo "Test Mode: $(grep -c 'USE_TEST_ADS.*true' app/build.gradle.kts)"
    echo ""
} > admob_report.txt

echo -e "${GREEN}✓ Reporte guardado en admob_report.txt${NC}"

# 7. Sugerencias
echo -e "\n${YELLOW}7. PRÓXIMOS PASOS:${NC}"
echo "1. Ejecuta: adb logcat | grep -E 'Ads|AdMob' para ver logs en tiempo real"
echo "2. Revisa admob_report.txt para detalles"
echo "3. Verifica tu cuenta de AdMob está activa y sin restricciones"

