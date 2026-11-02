$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$evidenceDir = Join-Path $projectRoot 'docs\evidence'
$latexEvidenceDir = Join-Path $evidenceDir 'latex'
New-Item -ItemType Directory -Force -Path $latexEvidenceDir | Out-Null
Push-Location $projectRoot

try {
    & (Join-Path $PSScriptRoot 'verify_windows.ps1')
    if (-not $?) {
        throw 'Windows build and test verification failed.'
    }

    $plantUmlJar = $env:PLANTUML_JAR
    if (-not $plantUmlJar -or -not (Test-Path -LiteralPath $plantUmlJar)) {
        $plantUmlCommand = Get-Command plantuml -ErrorAction SilentlyContinue
        if ($plantUmlCommand) {
            $plantUmlJar = $null
        }
        else {
            $toolDir = Join-Path $env:LOCALAPPDATA 'CS341TaskTracker\tools'
            New-Item -ItemType Directory -Force -Path $toolDir | Out-Null
            $plantUmlJar = Join-Path $toolDir 'plantuml-1.2025.10.jar'
            if (-not (Test-Path -LiteralPath $plantUmlJar)) {
                $jarUrl = 'https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2025.10/plantuml-1.2025.10.jar'
                $hashUrl = "$jarUrl.sha256"
                Invoke-WebRequest -UseBasicParsing -Uri $jarUrl -OutFile $plantUmlJar
                $expectedHashText = (Invoke-WebRequest -UseBasicParsing -Uri $hashUrl).Content.Trim()
                $expectedHash = ($expectedHashText -split '\s+')[0].ToUpperInvariant()
                $actualHash = (Get-FileHash -LiteralPath $plantUmlJar -Algorithm SHA256).Hash
                if ($expectedHash -notmatch '^[A-F0-9]{64}$' -or $actualHash -ne $expectedHash) {
                    Remove-Item -LiteralPath $plantUmlJar -Force
                    throw 'Downloaded PlantUML JAR failed its Maven Central SHA-256 check.'
                }
            }
        }
    }

    $plantUmlLog = Join-Path $evidenceDir 'plantuml-svg.log'
    "PlantUML source validation and rendering started $(Get-Date -Format o)" |
        Set-Content -LiteralPath $plantUmlLog -Encoding utf8
    if ($plantUmlJar) {
        & java -jar $plantUmlJar -charset UTF-8 -tsvg -o ..\diagrams docs\plantuml\*.puml *> $plantUmlLog
        $svgExit = $LASTEXITCODE
        & java -jar $plantUmlJar -charset UTF-8 -tpng -o ..\diagrams docs\plantuml\*.puml *> (Join-Path $evidenceDir 'plantuml-png.log')
        $pngExit = $LASTEXITCODE
    }
    else {
        & $plantUmlCommand.Source -charset UTF-8 -tsvg -o ..\diagrams docs\plantuml\*.puml *> $plantUmlLog
        $svgExit = $LASTEXITCODE
        & $plantUmlCommand.Source -charset UTF-8 -tpng -o ..\diagrams docs\plantuml\*.puml *> (Join-Path $evidenceDir 'plantuml-png.log')
        $pngExit = $LASTEXITCODE
    }
    if ($svgExit -ne 0 -or $pngExit -ne 0) {
        throw "PlantUML rendering failed: SVG exit=$svgExit, PNG exit=$pngExit."
    }
    $diagramSources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'docs\plantuml') -Filter '*.puml' -File
    foreach ($source in $diagramSources) {
        $baseName = [System.IO.Path]::GetFileNameWithoutExtension($source.Name)
        foreach ($extension in @('svg', 'png')) {
            $rendered = Join-Path $projectRoot "docs\diagrams\$baseName.$extension"
            if (-not (Test-Path -LiteralPath $rendered) -or (Get-Item -LiteralPath $rendered).Length -eq 0) {
                throw "Missing or empty PlantUML render: $rendered"
            }
        }
    }
    if ($diagramSources.Count -ne 7) {
        throw "Expected seven PlantUML sources; found $($diagramSources.Count)."
    }

    foreach ($tool in @('xelatex', 'pdftotext')) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw "Required PDF tool '$tool' was not found in PATH."
        }
    }
    $xelatexPath = (Get-Command xelatex).Source
    $documents = @('Project_Report_EN', 'Test_Report_EN', 'User_Guide_EN',
        'UML_Diagrams_EN', 'Presentation_EN', 'Gap_Audit_EN')
    $pdfResults = @()
    Push-Location (Join-Path $projectRoot 'docs\latex')
    try {
        foreach ($document in $documents) {
            for ($pass = 1; $pass -le 2; $pass++) {
                $logPath = Join-Path $latexEvidenceDir "$document-pass$pass.log"
                $latexCommand = 'cd /d "{0}" && "{1}" -interaction=nonstopmode -halt-on-error -file-line-error "{2}.tex" > "{3}" 2>&1' -f `
                    (Get-Location).Path, $xelatexPath, $document, $logPath
                & $env:ComSpec /c $latexCommand
                $latexExit = $LASTEXITCODE
                if ($latexExit -ne 0) {
                    throw "XeLaTeX failed for $document (pass $pass), exit code $latexExit. See $logPath"
                }
            }

            $sourcePdf = Join-Path (Get-Location).Path "$document.pdf"
            $destinationPdf = Join-Path $projectRoot "docs\$document.pdf"
            if (-not (Test-Path -LiteralPath $sourcePdf) -or (Get-Item -LiteralPath $sourcePdf).Length -eq 0) {
                throw "XeLaTeX did not produce a non-empty PDF: $sourcePdf"
            }
            Copy-Item -LiteralPath $sourcePdf -Destination $destinationPdf -Force

            $textPath = Join-Path $latexEvidenceDir "$document.txt"
            & pdftotext -layout -enc UTF-8 $destinationPdf $textPath
            $textExit = $LASTEXITCODE
            if ($textExit -ne 0 -or -not (Test-Path -LiteralPath $textPath) -or
                (Get-Item -LiteralPath $textPath).Length -eq 0) {
                throw "PDF text extraction failed for $destinationPdf."
            }
            $pdfText = Get-Content -LiteralPath $textPath -Raw
            $pageCount = [regex]::Matches($pdfText, "\f").Count
            if ($pageCount -lt 1) {
                throw "PDF page count could not be established for $destinationPdf."
            }
            $pdfResults += [pscustomobject]@{
                file = "docs/$document.pdf"
                bytes = (Get-Item -LiteralPath $destinationPdf).Length
                extractedPages = $pageCount
                textBytes = (Get-Item -LiteralPath $textPath).Length
                xelatexPasses = 2
                status = 'PASS'
            }
        }
    }
    finally {
        Pop-Location
    }
    $pdfResults | ConvertTo-Json -Depth 4 |
        Set-Content -LiteralPath (Join-Path $evidenceDir 'PDF_PREFLIGHT_EN.json') -Encoding utf8

    $pdftoppmCommand = Get-Command pdftoppm -ErrorAction SilentlyContinue
    if (-not $pdftoppmCommand) {
        throw 'pdftoppm is required to generate page contact sheets for visual inspection.'
    }
    Add-Type -AssemblyName System.Drawing
    $contactDir = Join-Path $evidenceDir 'pdf-contact-sheets'
    $renderTemp = Join-Path ([System.IO.Path]::GetTempPath()) ("cs341-pdf-pages-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $contactDir | Out-Null
    New-Item -ItemType Directory -Path $renderTemp | Out-Null
    try {
        foreach ($result in $pdfResults) {
            $document = [System.IO.Path]::GetFileNameWithoutExtension($result.file)
            $pdfPath = Join-Path $projectRoot $result.file.Replace('/', '\')
            $prefix = Join-Path $renderTemp $document
            $renderLog = Join-Path $latexEvidenceDir "$document-raster.log"
            $pdftoppmArgs = @('-r', '45', '-png', '-f', '1', '-l', "$($result.extractedPages)", $pdfPath, $prefix)
            & $pdftoppmCommand.Source @pdftoppmArgs *> $renderLog
            if ($LASTEXITCODE -ne 0) {
                throw "Could not rasterize all pages of $pdfPath. See $renderLog"
            }

            $pageImages = @(Get-ChildItem -LiteralPath $renderTemp -Filter "$document-*.png" -File | Sort-Object Name)
            if ($pageImages.Count -ne $result.extractedPages) {
                throw "Expected $($result.extractedPages) page images for $document; found $($pageImages.Count)."
            }

            $thumbWidth = 170
            $thumbHeight = 240
            $labelHeight = 28
            $gutter = 16
            $columns = 4
            $rows = [Math]::Ceiling($pageImages.Count / $columns)
            $sheet = [System.Drawing.Bitmap]::new(
                ($columns * ($thumbWidth + $gutter) + $gutter),
                ($rows * ($thumbHeight + $labelHeight + $gutter) + $gutter))
            $graphics = [System.Drawing.Graphics]::FromImage($sheet)
            $font = [System.Drawing.Font]::new('Arial', 10)
            try {
                $graphics.Clear([System.Drawing.Color]::White)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                for ($index = 0; $index -lt $pageImages.Count; $index++) {
                    $image = [System.Drawing.Image]::FromFile($pageImages[$index].FullName)
                    try {
                        $x = $gutter + ($index % $columns) * ($thumbWidth + $gutter)
                        $y = $gutter + [Math]::Floor($index / $columns) * ($thumbHeight + $labelHeight + $gutter)
                        $graphics.DrawString("Page $($index + 1)", $font,
                            [System.Drawing.Brushes]::Black, $x, $y)
                        $graphics.DrawImage($image, $x, ($y + $labelHeight), $thumbWidth, $thumbHeight)
                    }
                    finally {
                        $image.Dispose()
                    }
                }
                $contactPath = Join-Path $contactDir "$document-contact.png"
                $sheet.Save($contactPath, [System.Drawing.Imaging.ImageFormat]::Png)
            }
            finally {
                $font.Dispose()
                $graphics.Dispose()
                $sheet.Dispose()
            }
        }
    }
    finally {
        if (Test-Path -LiteralPath $renderTemp) {
            Remove-Item -LiteralPath $renderTemp -Recurse -Force
        }
    }

    $ignorePath = Join-Path $projectRoot '.gitignore'
    $ignoreText = Get-Content -LiteralPath $ignorePath -Raw
    foreach ($requiredPattern in @(
        'target/', 'tasks.db', '*.db-wal', '*.db-shm', '.env', '.env.*',
        '*.local', 'local.properties', '*.pem', '*.key', '*.p12', '*.pfx',
        'id_rsa', 'id_ed25519', '*.aux', 'docs/latex/*.log'
    )) {
        if ($ignoreText -notmatch [regex]::Escape($requiredPattern)) {
            throw ".gitignore is missing required pattern '$requiredPattern'."
        }
    }
    foreach ($forbiddenPattern in @('*.java', '*.puml', '*.tex', '*.pdf')) {
        if ($ignoreText -match "(?m)^\s*$([regex]::Escape($forbiddenPattern))\s*$") {
            throw ".gitignore must not exclude required source/deliverable pattern '$forbiddenPattern'."
        }
    }

    @(
        'PlantUML: PASS - 7 sources rendered as SVG and PNG.'
        'XeLaTeX: PASS - all six PDFs built twice and text-extracted.'
        'PDF page counts and sizes: docs/evidence/PDF_PREFLIGHT_EN.json'
        '.gitignore required generated/private-file patterns: PASS'
        'PDF page contact sheets: generated for all pages; human visual inspection is still required.'
        'Manual Swing GUI acceptance: NOT RUN by this script.'
    ) | Set-Content -LiteralPath (Join-Path $evidenceDir 'full-verification-status.txt') -Encoding utf8
    Get-Content -LiteralPath (Join-Path $evidenceDir 'full-verification-status.txt')
}
finally {
    Pop-Location
}
