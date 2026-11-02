$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$evidenceDir = Join-Path $projectRoot 'docs\evidence'
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
Push-Location $projectRoot

try {
    foreach ($tool in @('java', 'javac', 'mvn')) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw "Required command '$tool' was not found in PATH."
        }
    }

    $javaHome = $env:JAVA_HOME
    if (-not $javaHome -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
        $javaSettings = (& $env:ComSpec /c 'java -XshowSettings:properties -version 2>&1' | Out-String)
        $javaHomeMatch = [regex]::Match($javaSettings, 'java\.home\s*=\s*(.+)')
        if (-not $javaHomeMatch.Success) {
            throw "JAVA_HOME is invalid and the installed Java home could not be detected."
        }
        $javaHome = $javaHomeMatch.Groups[1].Value.Trim()
        if (-not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
            throw "Detected Java runtime '$javaHome' does not contain javac; install a JDK."
        }
        $env:JAVA_HOME = $javaHome
    }
    $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

    $environmentLog = Join-Path $evidenceDir 'environment-windows.txt'
    $environment = @(
        "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')"
        "OS: $([System.Runtime.InteropServices.RuntimeInformation]::OSDescription)"
        "JAVA_HOME: $env:JAVA_HOME"
        (& $env:ComSpec /c 'java -version 2>&1' | Out-String).Trim()
        (& $env:ComSpec /c 'javac -version 2>&1' | Out-String).Trim()
        (& $env:ComSpec /c 'mvn -version 2>&1' | Out-String).Trim()
    )
    $environment | Set-Content -LiteralPath $environmentLog -Encoding utf8

    $mavenLog = Join-Path $evidenceDir 'maven-clean-verify.log'
    & mvn -B clean verify *> $mavenLog
    $mavenExit = $LASTEXITCODE
    if ($mavenExit -ne 0) {
        throw "mvn -B clean verify failed with exit code $mavenExit. See $mavenLog"
    }

    $jarPath = Join-Path $projectRoot 'target\task-tracker-1.0.0.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "Expected executable JAR was not created: $jarPath"
    }
    $jarEntries = & jar tf $jarPath 2>&1
    if ($LASTEXITCODE -ne 0 -or $jarEntries -notcontains 'org/sqlite/JDBC.class' -or
        $jarEntries -notcontains 'META-INF/services/java.sql.Driver') {
        throw 'The packaged JAR is missing SQLite JDBC classes or service metadata.'
    }

    $smokeDir = Join-Path ([System.IO.Path]::GetTempPath()) ("cs341-jar-smoke-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $smokeDir | Out-Null
    try {
        $smokeSource = Join-Path $smokeDir 'JarSqliteSmoke.java'
        @'
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
public class JarSqliteSmoke {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + args[0]);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE smoke (value TEXT NOT NULL)");
            statement.execute("INSERT INTO smoke(value) VALUES ('ok')");
            try (ResultSet result = statement.executeQuery("SELECT value FROM smoke")) {
                if (!result.next() || !"ok".equals(result.getString(1))) {
                    throw new AssertionError("SQLite round trip failed");
                }
            }
        }
        System.out.println("PACKAGED_JAR_SQLITE_OK");
    }
}
'@ | Set-Content -LiteralPath $smokeSource -Encoding ascii
        $compileLog = Join-Path $smokeDir 'javac.log'
        & javac -cp $jarPath -d $smokeDir $smokeSource *> $compileLog
        if ($LASTEXITCODE -ne 0) {
            throw "Could not compile the packaged-JAR SQLite smoke test. See $compileLog"
        }
        $smokeDb = Join-Path $smokeDir 'smoke.db'
        $smokeRunLog = Join-Path $smokeDir 'smoke-run.log'
        & java -cp "$smokeDir;$jarPath" JarSqliteSmoke $smokeDb *> $smokeRunLog
        $smokeExit = $LASTEXITCODE
        $smokeOutput = Get-Content -LiteralPath $smokeRunLog
        $smokePassed = $smokeExit -eq 0 -and $smokeOutput -contains 'PACKAGED_JAR_SQLITE_OK'
        $smokeStatus = if ($smokePassed) { 'PASS' } else { 'FAIL' }
        $smokeReport = @(
            "Command: java -cp <temporary-classpath> JarSqliteSmoke <temporary-db>"
            "Exit code: $smokeExit"
            ($smokeOutput | Out-String).Trim()
            "Result: $smokeStatus"
        )
        $smokeReport | Set-Content -LiteralPath (Join-Path $evidenceDir 'jar-sqlite-smoke.txt') -Encoding utf8
        if (-not $smokePassed) {
            throw 'The packaged-JAR SQLite JDBC smoke test failed.'
        }
    }
    finally {
        if (Test-Path -LiteralPath $smokeDir) {
            Remove-Item -LiteralPath $smokeDir -Recurse -Force
        }
    }

    $pythonReport = Join-Path $evidenceDir 'python-sqlite-check.log'
    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($python) {
        & $python.Source 'scripts\sqlite_offline_check.py' *> $pythonReport
        if ($LASTEXITCODE -ne 0) {
            throw "Independent Python SQLite checks failed. See $pythonReport"
        }
    }
    else {
        'NOT RUN: Python was not found.' | Set-Content -LiteralPath $pythonReport -Encoding utf8
    }

    $testSuites = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'target\surefire-reports') -Filter 'TEST-*.xml' -File
    if (-not $testSuites) {
        throw 'Maven completed but no Surefire XML reports were found.'
    }
    $counts = [ordered]@{ tests = 0; failures = 0; errors = 0; skipped = 0 }
    foreach ($suiteFile in $testSuites) {
        [xml]$suite = Get-Content -LiteralPath $suiteFile.FullName -Raw
        foreach ($key in @($counts.Keys)) {
            $counts[$key] += [int]$suite.testsuite.GetAttribute($key)
        }
    }
    if ($counts.tests -lt 1 -or $counts.failures -ne 0 -or $counts.errors -ne 0) {
        throw "Surefire reported tests=$($counts.tests), failures=$($counts.failures), errors=$($counts.errors)."
    }

    @(
        'Windows local verification: PASS'
        "Maven: mvn -B clean verify (exit $mavenExit)"
        "Surefire: $($counts.tests) tests, $($counts.failures) failures, $($counts.errors) errors, $($counts.skipped) skipped"
        'Packaged JAR SQLite JDBC round trip: PASS'
        "Python SQLite check: $(if ($python) { 'executed; see python-sqlite-check.log' } else { 'NOT RUN' })"
    ) | Set-Content -LiteralPath (Join-Path $evidenceDir 'windows-verification-status.txt') -Encoding utf8

    Get-Content -LiteralPath (Join-Path $evidenceDir 'windows-verification-status.txt')
}
finally {
    Pop-Location
}
