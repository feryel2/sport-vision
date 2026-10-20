# Lancer depuis n'importe quel dossier. Aucun secret n'est affiché.
$ErrorActionPreference = 'Stop'
$backendDirectory = $PSScriptRoot
$localEnvironment = Join-Path $backendDirectory '.env.local'
$allowedNames = @('GEMINI_API_KEY', 'GEMINI_MODEL', 'GEMINI_TIMEOUT_SECONDS')
$previousValues = @{}
foreach ($variableName in $allowedNames) {
    $previousValues[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
}
try {
    if (Test-Path -LiteralPath $localEnvironment) {
        # NOM=VALEUR est du texte : aucune évaluation de code, aucun affichage des lignes.
        $lineNumber = 0
        foreach ($line in [IO.File]::ReadAllLines($localEnvironment)) {
            $lineNumber++
            if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
            if ($line -notmatch '^\s*([A-Z_]+)\s*=(.*)$' -or $Matches[1] -notin $allowedNames) {
                throw "Format ou variable non autorisée à la ligne $lineNumber du fichier local."
            }
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim(), 'Process')
        }
    }
    Push-Location -LiteralPath $backendDirectory
    try {
        $jarFile = Join-Path $backendDirectory 'target/sport-vision-backend-1.0.0.jar'
        if (-not (Test-Path -LiteralPath $jarFile)) { throw 'Compiler le backend avec mvn package avant le lancement.' }
        # Les secrets passent dans l'environnement hérité, jamais dans les arguments Java.
        & java '-Djava.net.preferIPv4Stack=true' -jar $jarFile `
            --spring.config.location=classpath:/ `
            --spring.profiles.active=integration `
            --server.address=127.0.0.1 --server.port=8080 `
            '--spring.datasource.url=jdbc:h2:mem:sportvision_integration_nour_feryel;DB_CLOSE_ON_EXIT=FALSE' `
            --spring.datasource.driver-class-name=org.h2.Driver `
            --spring.jpa.hibernate.ddl-auto=create-drop --spring.sql.init.mode=never `
            --training.demo.enabled=false --sportvision.demo.enabled=true
        if ($LASTEXITCODE -ne 0) { throw 'Le backend a arrêté son exécution avec une erreur.' }
    } finally { Pop-Location }
} finally {
    foreach ($variableName in $allowedNames) {
        [Environment]::SetEnvironmentVariable($variableName, $previousValues[$variableName], 'Process')
    }
}
