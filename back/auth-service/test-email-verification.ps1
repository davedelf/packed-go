# Script de prueba para verificación de email en auth-service
# Autor: PackedGo Team
# Fecha: 2025-12-14

Write-Host "🧪 TEST: Verificación de Email en Auth-Service" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Gray
Write-Host ""

# Configuración
$baseUrl = "http://localhost:8081/api"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# Test 1: Registro de Customer
Write-Host "📝 Test 1: Registrando nuevo CUSTOMER..." -ForegroundColor Yellow
$customerData = @{
    username = "testcustomer_$timestamp"
    email = "testcustomer_${timestamp}@test.com"
    document = [long](Get-Random -Minimum 30000000 -Maximum 40000000)
    password = "Test123456"
    name = "Test"
    lastName = "Customer"
    bornDate = "1990-01-15"
    telephone = [long](Get-Random -Minimum 1100000000 -Maximum 1199999999)
    gender = "MALE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/customer/register" `
        -Method Post `
        -Body $customerData `
        -ContentType "application/json"
    
    Write-Host "✅ Customer registrado exitosamente!" -ForegroundColor Green
    Write-Host "Respuesta: $($response | ConvertTo-Json)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE: Revisa el email testcustomer_${timestamp}@test.com" -ForegroundColor Magenta
    Write-Host "   Se debe haber enviado un email de verificación." -ForegroundColor Magenta
    Write-Host ""
} catch {
    Write-Host "❌ Error al registrar customer: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 2: Registro de Admin
Write-Host "📝 Test 2: Registrando nuevo ADMIN..." -ForegroundColor Yellow
$adminData = @{
    username = "testadmin_$timestamp"
    email = "testadmin_${timestamp}@test.com"
    password = "Admin123456"
    authorizationCode = "PACKEDGO-ADMIN-2025"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/admin/register" `
        -Method Post `
        -Body $adminData `
        -ContentType "application/json"
    
    Write-Host "✅ Admin registrado exitosamente!" -ForegroundColor Green
    Write-Host "Respuesta: $($response | ConvertTo-Json)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE: Revisa el email testadmin_${timestamp}@test.com" -ForegroundColor Magenta
    Write-Host "   Se debe haber enviado un email de verificación." -ForegroundColor Magenta
    Write-Host ""
} catch {
    Write-Host "❌ Error al registrar admin: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 3: Intentar login sin verificar email (Customer)
Write-Host "📝 Test 3: Intentando login de CUSTOMER sin verificar email..." -ForegroundColor Yellow
$loginData = @{
    document = [long]$($customerData | ConvertFrom-Json).document
    password = "Test123456"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/customer/login" `
        -Method Post `
        -Body $loginData `
        -ContentType "application/json"
    
    Write-Host "❌ Login exitoso (NO DEBERÍA PERMITIR!)" -ForegroundColor Red
    Write-Host "Respuesta: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    if ($_.Exception.Message -like "*verify your email*" -or $_.ErrorDetails.Message -like "*verify your email*") {
        Write-Host "✅ Login bloqueado correctamente - Email no verificado" -ForegroundColor Green
        Write-Host "Mensaje de error esperado: $($_.ErrorDetails.Message)" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  Error inesperado: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "=" * 60 -ForegroundColor Gray
Write-Host "🏁 Tests completados" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 SIGUIENTE PASO:" -ForegroundColor Yellow
Write-Host "1. Revisa los logs del auth-service:" -ForegroundColor White
Write-Host "   docker-compose logs -f auth-service | Select-String 'Verification email'" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Verifica que se haya generado el token en la base de datos:" -ForegroundColor White
Write-Host "   docker exec -it back-auth-db-1 psql -U auth_user -d auth_db -c 'SELECT * FROM email_verification_tokens ORDER BY created_at DESC LIMIT 2;'" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Si usas Gmail real, revisa la bandeja de entrada de:" -ForegroundColor White
Write-Host "   - testcustomer_${timestamp}@test.com" -ForegroundColor Gray
Write-Host "   - testadmin_${timestamp}@test.com" -ForegroundColor Gray
Write-Host ""
