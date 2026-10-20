# Script de configuration sécurisé de la clé d'API Gemini
$ErrorActionPreference = 'Stop'

# Résolution automatique du dossier backend (qu'il soit collé dans la console ou exécuté en script)
$backendDirectory = if ($PSScriptRoot) {
    $PSScriptRoot
} elseif (Test-Path "backend") {
    (Resolve-Path "backend").Path
} else {
    $PWD.Path
}
$envLocalPath = Join-Path $backendDirectory '.env.local'

Write-Host "=== Configuration de la clé d'API Gemini (Sport Vision) ===" -ForegroundColor Cyan
Write-Host "Le fichier sera enregistré dans : $envLocalPath" -ForegroundColor Gray
Write-Host "Pour obtenir une clé gratuite : https://aistudio.google.com/app/apikey`n" -ForegroundColor Yellow

$secureKey = Read-Host -Prompt "Entrez votre clé API Gemini (saisie masquée)" -AsSecureString
if ($null -eq $secureKey -or $secureKey.Length -eq 0) {
    Write-Host "Aucune clé saisie. Opération annulée." -ForegroundColor Red
    return
}

$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
$apiKey = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr).Trim()
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

$lines = @(
    "GEMINI_API_KEY=$apiKey",
    "GEMINI_MODEL=gemini-2.5-flash",
    "GEMINI_TIMEOUT_SECONDS=45"
)

[IO.File]::WriteAllLines($envLocalPath, $lines)
Write-Host "`n[SUCCES] Clé enregistrée dans backend/.env.local" -ForegroundColor Green
Write-Host "Le module produit et les recommandations IA utiliseront cette clé." -ForegroundColor Green