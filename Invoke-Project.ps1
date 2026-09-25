[CmdletBinding()]
param(
    [ValidateSet('Targeted', 'Full')]
    [string]$Preset = 'Targeted'
)

Set-StrictMode -Version 2
$ErrorActionPreference = 'Stop'
$script:RepoRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $MyInvocation.MyCommand.Path)).TrimEnd([char]92, [char]47)
$script:AllowedTextExtensions = @('.java', '.xml', '.yml', '.yaml', '.json', '.md', '.ps1', '.psm1', '.properties', '.cmd', '.sh', '.toml', '.txt', '.sql')

function Write-GatePass {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ("PASS: " + $Message)
}

function Invoke-NativeCapture {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $false)][string[]]$Arguments = @()
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $capturedOutput = @(& $Executable @Arguments 2>&1)
        $capturedExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    return [pscustomobject]@{
        ExitCode = [int]$capturedExitCode
        Output = [object[]]$capturedOutput
    }
}

function Invoke-GitCapture {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $gitArguments = @('-C', $script:RepoRoot) + $Arguments
    $result = Invoke-NativeCapture -Executable 'git.exe' -Arguments $gitArguments
    if ($result.ExitCode -ne 0) {
        $text = [string]::Join([Environment]::NewLine, [string[]]$result.Output)
        throw ("git " + ($Arguments -join ' ') + " failed with exit code " + $result.ExitCode + ". " + $text)
    }

    return ,([string[]]$result.Output)
}

function Test-RepositoryPreflight {
    $gitRootOutput = Invoke-GitCapture -Arguments @('rev-parse', '--show-toplevel')
    $gitRoot = [System.IO.Path]::GetFullPath(([string]$gitRootOutput[0]).Trim()).TrimEnd([char]92, [char]47)
    if (-not [string]::Equals($gitRoot, $script:RepoRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "The script directory is not the Git repository root."
    }

    $branchOutput = Invoke-GitCapture -Arguments @('branch', '--show-current')
    $headOutput = Invoke-GitCapture -Arguments @('rev-parse', 'HEAD')
    $branch = ([string]$branchOutput[0]).Trim()
    $head = ([string]$headOutput[0]).Trim()
    if ([string]::IsNullOrWhiteSpace($branch) -or [string]::IsNullOrWhiteSpace($head)) {
        throw "Git branch or HEAD could not be identified."
    }
    Write-Host ("Repository branch: " + $branch)
    Write-Host ("Repository HEAD: " + $head)

    $diffResult = Invoke-NativeCapture -Executable 'git.exe' -Arguments @('-C', $script:RepoRoot, 'diff', '--check')
    $diffResult.Output | ForEach-Object { Write-Host ([string]$_) }
    if ($diffResult.ExitCode -ne 0) {
        throw "Unstaged diff whitespace validation failed."
    }

    $cachedDiffResult = Invoke-NativeCapture -Executable 'git.exe' -Arguments @('-C', $script:RepoRoot, 'diff', '--cached', '--check')
    $cachedDiffResult.Output | ForEach-Object { Write-Host ([string]$_) }
    if ($cachedDiffResult.ExitCode -ne 0) {
        throw "Staged diff whitespace validation failed."
    }

    Write-GatePass "Git root, branch, HEAD, and diff whitespace checks"
}

function Test-PowerShellSyntax {
    $repositoryEntries = Invoke-GitCapture -Arguments @('ls-files', '--cached', '--others', '--exclude-standard')
    $scriptPaths = @($repositoryEntries | Where-Object { [System.IO.Path]::GetExtension(([string]$_)) -eq '.ps1' })
    $parsedCount = 0
    foreach ($relativePath in $scriptPaths) {
        $candidatePath = Join-Path $script:RepoRoot ([string]$relativePath)
        if (-not (Test-Path -LiteralPath $candidatePath -PathType Leaf)) {
            continue
        }
        $tokens = $null
        $parseErrors = $null
        [System.Management.Automation.Language.Parser]::ParseFile(
            $candidatePath,
            [ref]$tokens,
            [ref]$parseErrors
        ) | Out-Null
        if ($parseErrors.Count -gt 0) {
            $details = ($parseErrors | ForEach-Object { $_.Message + " at line " + $_.Extent.StartLineNumber }) -join '; '
            throw ("PowerShell parse failed for " + $relativePath + ": " + $details)
        }
        $parsedCount++
    }
    Write-GatePass ("PowerShell 5.1 parser accepted " + $parsedCount + " script(s)")
}

function Test-RepositoryTextHygiene {
    $entries = Invoke-GitCapture -Arguments @('ls-files', '--cached', '--others', '--exclude-standard')
    $personalPathPattern = '(?i)\b[A-Z]:[/\\]Users[/\\][^/\\\s]+|(?:^|\s)/Users/[^/\s]+|(?:^|\s)/home/[^/\s]+'
    $secretPatterns = @(
        '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
        '\bgh[pousr]_[A-Za-z0-9_]{30,}\b',
        '\bgithub_pat_[A-Za-z0-9_]{20,}\b',
        '\bAKIA[0-9A-Z]{16}\b',
        '\bAIza[0-9A-Za-z_-]{35}\b'
    )

    foreach ($entry in $entries) {
        $relativePath = ([string]$entry).Trim()
        if ([string]::IsNullOrWhiteSpace($relativePath)) {
            continue
        }
        $normalizedRelativePath = $relativePath -replace '\\', '/'
        if ($normalizedRelativePath -match '^(?:target|\.git)/') {
            continue
        }
        $filePath = Join-Path $script:RepoRoot $relativePath
        if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) {
            continue
        }
        $extension = [System.IO.Path]::GetExtension($filePath).ToLowerInvariant()
        if ($script:AllowedTextExtensions -notcontains $extension -and $normalizedRelativePath -ne 'mvnw') {
            continue
        }

        $content = [System.IO.File]::ReadAllText($filePath)
        if ($content -match $personalPathPattern) {
            throw ("Personal machine path found in " + $relativePath)
        }
        foreach ($pattern in $secretPatterns) {
            if ($content -match $pattern) {
                throw ("Secret-like value found in " + $relativePath)
            }
        }
    }

    $ignoreResult = Invoke-NativeCapture -Executable 'git.exe' -Arguments @('-C', $script:RepoRoot, 'check-ignore', '-q', '--', 'AGENTS.local.md')
    if ($ignoreResult.ExitCode -ne 0) {
        throw "AGENTS.local.md must be ignored by Git."
    }
    Write-GatePass "Tracked and untracked text has no detected machine paths or common credentials"
}

function Test-ProjectMetadata {
    $requiredPaths = @(
        'AGENTS.md',
        '.gitattributes',
        '.gitignore',
        '.mvn/wrapper/maven-wrapper.properties',
        'mvnw',
        'mvnw.cmd',
        'pom.xml',
        'docs/PORTFOLIO_COMPLETENESS.md',
        'src/main/java/com/leadintake/api/LeadIntakeApiApplication.java',
        'src/main/resources/application.yml'
    )
    foreach ($relativePath in $requiredPaths) {
        if (-not (Test-Path -LiteralPath (Join-Path $script:RepoRoot $relativePath) -PathType Leaf)) {
            throw ("Required project file is missing: " + $relativePath)
        }
    }

    $pomPath = Join-Path $script:RepoRoot 'pom.xml'
    $pom = New-Object System.Xml.XmlDocument
    $pom.Load($pomPath)
    $namespace = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
    $namespace.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
    $artifactIdNode = $pom.SelectSingleNode('/m:project/m:artifactId', $namespace)
    $versionNode = $pom.SelectSingleNode('/m:project/m:version', $namespace)
    if ($null -eq $artifactIdNode -or $null -eq $versionNode) {
        throw "Maven artifact identity is incomplete."
    }

    $mapPath = Join-Path $script:RepoRoot 'docs/PORTFOLIO_COMPLETENESS.md'
    $map = [System.IO.File]::ReadAllText($mapPath)
    $horizontalCount = [regex]::Matches($map, '(?m)^\| H\d{2} \|').Count
    $verticalCount = [regex]::Matches($map, '(?m)^\| V\d{2} \|').Count
    if ($horizontalCount -ne 23 -or $verticalCount -ne 7) {
        throw ("Completion map must cover H01-H23 and V01-V07; found " + $horizontalCount + " horizontal and " + $verticalCount + " vertical rows.")
    }

    $wrapperProperties = [System.IO.File]::ReadAllText((Join-Path $script:RepoRoot '.mvn/wrapper/maven-wrapper.properties'))
    if ($wrapperProperties -notmatch '(?m)^distributionSha256Sum=[0-9a-f]{64}\r?$') {
        throw "Maven distribution SHA-256 pin is missing or malformed."
    }

    Write-GatePass ("Project files and Maven identity " + $artifactIdNode.InnerText + ":" + $versionNode.InnerText)
    Write-GatePass "Completion map includes H01-H23 and V01-V07"
}

function Test-JavaToolchain {
    $javaResult = Invoke-NativeCapture -Executable 'java.exe' -Arguments @('--version')
    $javaText = [string]::Join([Environment]::NewLine, [string[]]$javaResult.Output)
    if ($javaResult.ExitCode -ne 0) {
        throw ("java --version failed with exit code " + $javaResult.ExitCode + ". " + $javaText)
    }
    if ($javaText -notmatch '(?i)(?:openjdk|java)\s+(?:version\s+)?["]?21(?:[.+\-\s]|$)') {
        throw "Java 21 is required by this project."
    }

    $javacResult = Invoke-NativeCapture -Executable 'javac.exe' -Arguments @('--version')
    $javacText = [string]::Join([Environment]::NewLine, [string[]]$javacResult.Output)
    if ($javacResult.ExitCode -ne 0 -or $javacText -notmatch '(?i)\bjavac\s+21(?:[.+\-\s]|$)') {
        throw ("javac 21 is required. " + $javacText)
    }

    Write-GatePass "Java and javac both report version 21"
}

function Invoke-MavenBuild {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $wrapperPath = Join-Path $script:RepoRoot 'mvnw.cmd'
    $originalMavenOpts = $env:MAVEN_OPTS
    $result = Invoke-NativeCapture -Executable $wrapperPath -Arguments $Arguments
    if ($result.ExitCode -eq 0) {
        $result.Output | ForEach-Object { Write-Host ([string]$_) }
        return
    }

    $failureText = [string]::Join([Environment]::NewLine, [string[]]$result.Output)
    $runningOnWindows = $env:OS -eq 'Windows_NT'
    $isCertificateFailure = $failureText -match '(?i)(PKIX path building failed|certificate_unknown)'
    $alreadyUsingWindowsRoots = $originalMavenOpts -match 'trustStoreType=Windows-ROOT'
    if ($runningOnWindows -and $isCertificateFailure -and -not $alreadyUsingWindowsRoots) {
        Write-Host "Maven could not validate the dependency server certificate with Java's default trust store; retrying with the existing Windows Root trust store for this process."
        $fallbackOptions = (($originalMavenOpts + ' -Djavax.net.ssl.trustStore=NONE -Djavax.net.ssl.trustStoreType=Windows-ROOT').Trim())
        try {
            $env:MAVEN_OPTS = $fallbackOptions
            $retryResult = Invoke-NativeCapture -Executable $wrapperPath -Arguments $Arguments
        }
        finally {
            $env:MAVEN_OPTS = $originalMavenOpts
        }
        if ($retryResult.ExitCode -eq 0) {
            $retryResult.Output | ForEach-Object { Write-Host ([string]$_) }
            return
        }
        $result.Output | ForEach-Object { Write-Host ([string]$_) }
        $retryResult.Output | ForEach-Object { Write-Host ([string]$_) }
        throw ("Maven failed after the Windows trust-store retry with exit code " + $retryResult.ExitCode + ".")
    }

    $result.Output | ForEach-Object { Write-Host ([string]$_) }
    throw ("Maven failed with exit code " + $result.ExitCode + ".")
}

function Test-DockerEngine {
    $docker = Get-Command docker.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $docker) {
        return $false
    }

    $dockerResult = Invoke-NativeCapture -Executable $docker.Source -Arguments @('info', '--format', '{{.ServerVersion}}')
    if ($dockerResult.ExitCode -eq 0 -and $dockerResult.Output.Count -gt 0) {
        Write-GatePass "Docker Engine is reachable"
        return $true
    }

    return $false
}

function Test-PackagedArtifact {
    param([Parameter(Mandatory = $true)][datetime]$BuildStarted)

    $pom = New-Object System.Xml.XmlDocument
    $pom.Load((Join-Path $script:RepoRoot 'pom.xml'))
    $namespace = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
    $namespace.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
    $artifactId = $pom.SelectSingleNode('/m:project/m:artifactId', $namespace).InnerText
    $version = $pom.SelectSingleNode('/m:project/m:version', $namespace).InnerText
    $jarPath = Join-Path $script:RepoRoot ("target/" + $artifactId + "-" + $version + ".jar")
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw ("Expected Spring Boot artifact was not produced: " + $artifactId + "-" + $version + ".jar")
    }
    $jar = Get-Item -LiteralPath $jarPath
    if ($jar.Length -le 0 -or $jar.LastWriteTime -lt $BuildStarted.AddSeconds(-3)) {
        throw "Packaged artifact is empty or older than the current Full validation run."
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $manifestEntry = $archive.GetEntry('META-INF/MANIFEST.MF')
        if ($null -eq $manifestEntry) {
            throw "Packaged artifact has no manifest."
        }
        $reader = New-Object System.IO.StreamReader($manifestEntry.Open())
        try {
            $manifest = $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $archive.Dispose()
    }

    if ($manifest -notmatch '(?m)^Start-Class: com\.leadintake\.api\.LeadIntakeApiApplication\r?$') {
        throw "Packaged artifact does not identify the expected Spring Boot main class."
    }
    if ($manifest -notmatch ('(?m)^Implementation-Version: ' + [regex]::Escape($version) + '\r?$')) {
        throw "Packaged artifact version does not match pom.xml."
    }
    Write-GatePass ("Fresh Spring Boot artifact identity " + $artifactId + ":" + $version)
}

function Invoke-ProjectValidation {
    Test-RepositoryPreflight
    Test-PowerShellSyntax
    Test-RepositoryTextHygiene
    Test-ProjectMetadata
    Test-JavaToolchain

    if ($Preset -eq 'Targeted') {
        Invoke-MavenBuild -Arguments @('-B', '-ntp', '-DskipITs=true', 'test')
        Write-GatePass "Targeted validation completed"
        return 0
    }

    $testSourceRoot = Join-Path $script:RepoRoot 'src/test/java'
    $integrationFiles = @()
    if (Test-Path -LiteralPath $testSourceRoot -PathType Container) {
        $integrationFiles = @(Get-ChildItem -LiteralPath $testSourceRoot -Filter '*PostgresIT.java' -Recurse -File)
    }
    $dockerReachable = Test-DockerEngine
    $buildStarted = Get-Date
    $mavenArguments = @('-B', '-ntp')
    if (-not $dockerReachable -or $integrationFiles.Count -eq 0) {
        $mavenArguments += '-DskipITs=true'
    }
    $mavenArguments += @('clean', 'verify')
    Invoke-MavenBuild -Arguments $mavenArguments
    Test-PackagedArtifact -BuildStarted $buildStarted

    $blockedReasons = @()
    if ($integrationFiles.Count -eq 0) {
        $blockedReasons += "PostgreSQL Testcontainers integration test is not configured yet."
    }
    if (-not $dockerReachable) {
        $blockedReasons += "PostgreSQL integration proof not run because Docker is unavailable."
    }
    if ($blockedReasons.Count -gt 0) {
        foreach ($reason in $blockedReasons) {
            Write-Host ("BLOCKED: " + $reason)
        }
        return 2
    }

    Write-GatePass "Full local validation completed, including PostgreSQL integration proof and packaging"
    return 0
}

try {
    $result = Invoke-ProjectValidation
    exit $result
}
catch {
    Write-Host ("FAILED: " + $_.Exception.Message)
    exit 1
}
