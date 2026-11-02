$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$evidenceDir = Join-Path $projectRoot 'docs\evidence'
$distDir = Join-Path $projectRoot 'dist'
$archivePath = Join-Path $distDir 'CS341_Task_Tracker_FINAL.zip'
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
Push-Location $projectRoot

function New-ProjectArchive {
    param(
        [string]$Root,
        [string]$Destination
    )

    if (Test-Path -LiteralPath $Destination) {
        throw "Refusing to overwrite an existing release archive: $Destination"
    }

    $files = Get-ChildItem -LiteralPath $Root -Recurse -File | Where-Object {
        $relative = $_.FullName.Substring($Root.Length).TrimStart('\')
        $relative -notmatch '(^|\\)(target|dist|\.git)(\\|$)' -and
        $relative -notmatch '(^|\\)\.env($|\\)' -and
        $relative -notin @(
            'docs\evidence\release-extracted-build.log',
            'docs\evidence\release-final-extracted-build.log',
            'docs\evidence\release-zip-verification.txt'
        ) -and
        $_.Name -notmatch '(^id_rsa$|^id_ed25519$|\.pem$|\.p12$|\.pfx$|\.key$)' -and
        $_.Extension -notin @('.class', '.db', '.jar') -and
        $relative -notmatch '^docs\\latex\\.*\.pdf$' -and
        $relative -notmatch '^docs\\latex\\.*\.(aux|log|out|toc|nav|snm|fdb_latexmk|fls)$' -and
        $relative -notmatch '^docs\\latex\\.*\.synctex\.gz$' -and
        $relative -notmatch '(^|\\)[^\\]+\.db-(journal|shm|wal)$'
    }
    if (-not $files) {
        throw 'No project files were selected for the release archive.'
    }

    $fileStream = [System.IO.File]::Open($Destination, [System.IO.FileMode]::CreateNew)
    try {
        $archive = [System.IO.Compression.ZipArchive]::new(
            $fileStream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            foreach ($file in $files) {
                $relative = $file.FullName.Substring($Root.Length).TrimStart('\').Replace('\', '/')
                $entry = $archive.CreateEntry(
                    "CS341_Task_Tracker/$relative",
                    [System.IO.Compression.CompressionLevel]::Optimal)
                $sourceStream = [System.IO.File]::OpenRead($file.FullName)
                try {
                    $entryStream = $entry.Open()
                    try {
                        $sourceStream.CopyTo($entryStream)
                    }
                    finally {
                        $entryStream.Dispose()
                    }
                }
                finally {
                    $sourceStream.Dispose()
                }
            }
        }
        finally {
            $archive.Dispose()
        }
    }
    finally {
        $fileStream.Dispose()
    }
    return $files.Count
}

function Test-ArchiveEntries {
    param([string]$Path)

    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $names = @($zip.Entries | ForEach-Object { $_.FullName })
        foreach ($required in @(
            'CS341_Task_Tracker/pom.xml',
            'CS341_Task_Tracker/README.md',
            'CS341_Task_Tracker/docs/Project_Report_EN.pdf',
            'CS341_Task_Tracker/docs/Test_Report_EN.pdf',
            'CS341_Task_Tracker/docs/User_Guide_EN.pdf',
            'CS341_Task_Tracker/docs/UML_Diagrams_EN.pdf',
            'CS341_Task_Tracker/docs/Presentation_EN.pdf',
            'CS341_Task_Tracker/docs/Gap_Audit_EN.pdf',
            'CS341_Task_Tracker/docs/plantuml/class.puml',
            'CS341_Task_Tracker/docs/plantuml/er.puml',
            'CS341_Task_Tracker/docs/plantuml/use_case.puml',
            'CS341_Task_Tracker/docs/plantuml/component.puml',
            'CS341_Task_Tracker/docs/plantuml/sequence.puml',
            'CS341_Task_Tracker/docs/plantuml/activity.puml',
            'CS341_Task_Tracker/docs/plantuml/deployment.puml'
        )) {
            if ($names -notcontains $required) {
                throw "Release archive is missing required file: $required"
            }
        }

        foreach ($entry in $zip.Entries) {
            $entryStream = $entry.Open()
            try {
                $buffer = New-Object byte[] 81920
                while ($entryStream.Read($buffer, 0, $buffer.Length) -gt 0) { }
            }
            finally {
                $entryStream.Dispose()
            }
        }
        return $names.Count
    }
    finally {
        $zip.Dispose()
    }
}

try {
    & (Join-Path $PSScriptRoot 'verify_all_windows.ps1')
    if (-not $?) {
        throw 'Full local verification failed; release packaging was stopped.'
    }

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $includedCount = New-ProjectArchive -Root $projectRoot -Destination $archivePath

    $extractRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("cs341-release-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $extractRoot | Out-Null
    try {
        Expand-Archive -LiteralPath $archivePath -DestinationPath $extractRoot
        $extractedProject = Join-Path $extractRoot 'CS341_Task_Tracker_English_FINAL'
        if (-not (Test-Path -LiteralPath (Join-Path $extractedProject 'pom.xml'))) {
            throw 'Extracted release did not contain pom.xml.'
        }

        Push-Location $extractedProject
        try {
            & mvn -B clean verify *> (Join-Path $evidenceDir 'release-extracted-build.log')
            $extractedBuildExit = $LASTEXITCODE
        }
        finally {
            Pop-Location
        }
        if ($extractedBuildExit -ne 0) {
            throw "Extracted release build failed with exit code $extractedBuildExit."
        }
    }
    finally {
        if (Test-Path -LiteralPath $extractRoot) {
            Remove-Item -LiteralPath $extractRoot -Recurse -Force
        }
    }

    $entryCount = Test-ArchiveEntries -Path $archivePath

    $hash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash
    @(
        "Archive: $archivePath"
        "Included project files: $includedCount"
        "ZIP entries checked: $entryCount"
        "Extracted Maven build: PASS (exit $extractedBuildExit)"
        "SHA-256: $hash"
    ) | Set-Content -LiteralPath (Join-Path $evidenceDir 'release-zip-verification.txt') -Encoding utf8
    @(
        "SHA256  $hash  dist/CS341_Task_Tracker_FINAL.zip"
    ) | Set-Content -LiteralPath (Join-Path $distDir 'SHA256_MANIFEST.txt') -Encoding utf8
    Get-Content -LiteralPath (Join-Path $evidenceDir 'release-zip-verification.txt')
}
finally {
    Pop-Location
}
