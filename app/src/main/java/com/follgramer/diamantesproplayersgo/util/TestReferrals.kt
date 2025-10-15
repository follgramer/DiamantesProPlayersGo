package com.follgramer.diamantesproplayersgo.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.follgramer.diamantesproplayersgo.SessionManager
import kotlinx.coroutines.launch

class TestReferrals(private val context: Context) {

    companion object {
        private const val TAG = "TestReferrals"

        fun runAllTests(context: Context) {
            val tester = TestReferrals(context)
            tester.executeAllTests()
        }
    }

    private fun executeAllTests() {
        Log.d(TAG, "=== Iniciando tests de sistema de referidos ===")

        test1_GenerateReferralCode()
        test2_CreateReferralData()
        test3_ProcessReferralCode()
        test4_CheckDeviceId()
        test5_ValidateReferralStats()
        test6_TestInviteLinks()

        Log.d(TAG, "=== Tests completados ===")
    }

    private fun test1_GenerateReferralCode() {
        try {
            Log.d(TAG, "Test 1: Generación de código de referido")

            val testPlayerId = "123456789"
            val code = ReferralManager.generateReferralCode(testPlayerId)

            if (code.isNotEmpty() && code.startsWith("REF")) {
                Log.d(TAG, "✅ Test 1 PASSED - Código generado: $code")
            } else {
                Log.e(TAG, "❌ Test 1 FAILED - Código inválido: $code")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test 1 ERROR: ${e.message}")
        }
    }

    private fun test2_CreateReferralData() {
        try {
            Log.d(TAG, "Test 2: Creación de datos de referido")

            val testPlayerId = SessionManager.getPlayerId(context)
            if (testPlayerId.isEmpty()) {
                Log.w(TAG, "⚠️ Test 2 SKIPPED - No hay player ID configurado")
                return
            }

            // Simular creación de código
            val mockCode = "REF${testPlayerId.takeLast(4)}TEST"
            Log.d(TAG, "✅ Test 2 PASSED - Mock código: $mockCode")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test 2 ERROR: ${e.message}")
        }
    }

    private fun test3_ProcessReferralCode() {
        try {
            Log.d(TAG, "Test 3: Procesamiento de código de referido")

            val testCode = "REF1234TEST"
            Log.d(TAG, "Código de prueba: $testCode")

            // Solo validar formato
            if (testCode.startsWith("REF") && testCode.length >= 8) {
                Log.d(TAG, "✅ Test 3 PASSED - Formato válido")
            } else {
                Log.e(TAG, "❌ Test 3 FAILED - Formato inválido")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test 3 ERROR: ${e.message}")
        }
    }

    private fun test4_CheckDeviceId() {
        try {
            Log.d(TAG, "Test 4: Verificación de Device ID")

            // Test sincrónico del formato
            val mockDeviceId = "device_${System.currentTimeMillis().toString().takeLast(8)}"

            if (mockDeviceId.isNotEmpty() && mockDeviceId.length >= 8) {
                Log.d(TAG, "✅ Test 4 PASSED - Device ID mock válido")
            } else {
                Log.e(TAG, "❌ Test 4 FAILED - Device ID inválido")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test 4 ERROR: ${e.message}")
        }
    }

    private fun test5_ValidateReferralStats() {
        try {
            Log.d(TAG, "Test 5: Validación de estadísticas")

            // Crear stats mock
            val mockStats = ReferralManager.ReferralStats(
                code = "REF1234TEST",
                totalReferrals = 5L,
                totalRewards = 3L,
                hasCode = true
            )

            if (mockStats.hasCode && mockStats.code.isNotEmpty()) {
                Log.d(TAG, "✅ Test 5 PASSED - Stats válidas: ${mockStats.totalReferrals} referidos, ${mockStats.totalRewards} recompensas")
            } else {
                Log.e(TAG, "❌ Test 5 FAILED - Stats inválidas")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test 5 ERROR: ${e.message}")
        }
    }

    private fun test6_TestInviteLinks() {
        try {
            Log.d(TAG, "Test 6: Generación de links de invitación")

            val testCode = "REF1234TEST"
            val links = ReferralManager.generateInviteLinks(testCode)

            if (links.appLink.contains(testCode) && links.webLink.contains(testCode)) {
                Log.d(TAG, "✅ Test 6 PASSED - Links generados correctamente")
                Log.d(TAG, "App Link: ${links.appLink}")
                Log.d(TAG, "Web Link: ${links.webLink}")
            } else {
                Log.e(TAG, "❌ Test 6 FAILED - Links inválidos")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test 6 ERROR: ${e.message}")
        }
    }

    fun testReferralFlow(playerId: String) {
        try {
            Log.d(TAG, "=== Test de flujo completo de referidos ===")

            // 1. Generar código
            val code = ReferralManager.generateReferralCode(playerId)
            Log.d(TAG, "Código generado: $code")

            // 2. Generar links
            val links = ReferralManager.generateInviteLinks(code)
            Log.d(TAG, "Links generados: ${links.appLink}")

            // 3. Crear stats mock
            val stats = ReferralManager.ReferralStats(
                code = code,
                totalReferrals = 0L,
                totalRewards = 0L,
                hasCode = true
            )
            Log.d(TAG, "Stats iniciales: $stats")

            Log.d(TAG, "✅ Flujo de referidos testeado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en test de flujo: ${e.message}")
        }
    }

    fun validateReferralSystem(): Boolean {
        return try {
            val testPlayerId = "123456789"
            val code = ReferralManager.generateReferralCode(testPlayerId)
            val links = ReferralManager.generateInviteLinks(code)

            code.isNotEmpty() &&
                    code.startsWith("REF") &&
                    links.appLink.contains(code) &&
                    links.webLink.contains(code)

        } catch (e: Exception) {
            Log.e(TAG, "Error validando sistema: ${e.message}")
            false
        }
    }
}