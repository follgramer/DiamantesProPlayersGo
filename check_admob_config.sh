#!/bin/bash

echo "==== VERIFICACIÓN DE CONFIGURACIÓN ADMOB ===="
echo ""

# 1. Verificar manifest placeholders en build.gradle.kts
echo "1. Verificando manifestPlaceholders en build.gradle.kts:"
if grep -q "manifestPlaceholders" app/build.gradle.kts; then
    echo "✅ manifestPlaceholders encontrado:"
    grep -A2 "manifestPlaceholders" app/build.gradle.kts
else
    echo "❌ NO HAY manifestPlaceholders - ESTO ES CRÍTICO"
    echo "   Agregar en defaultConfig:"
    echo '   manifestPlaceholders["admobAppId"] = "ca-app-pub-2024712392092488~7992650364"'
fi

echo ""
echo "2. Verificando meta-data en AndroidManifest.xml:"
grep -A1 "APPLICATION_ID" app/src/main/AndroidManifest.xml

echo ""
echo "3. Verificando inicialización de MobileAds:"
if grep -q "MobileAds.initialize" app/src/main/java/com/follgramer/diamantesproplayersgo/ads/AdsInit.kt; then
    echo "✅ MobileAds.initialize encontrado"
else
    echo "❌ MobileAds.initialize NO encontrado"
fi

echo ""
echo "4. Verificando verificación de consentimiento:"
if grep -q "canRequestAds()" app/src/main/java/com/follgramer/diamantesproplayersgo/ads/BannerHelper.kt; then
    echo "✅ canRequestAds() verificación encontrada"
else
    echo "❌ canRequestAds() NO verificado antes de cargar anuncios"
fi

echo ""
echo "5. Verificando modo de compilación:"
if grep -q "buildConfigField.*USE_TEST_ADS.*true" app/build.gradle.kts; then
    echo "⚠️ Modo DEBUG con anuncios de prueba activado"
else
    echo "✅ Modo RELEASE con anuncios reales"
fi

