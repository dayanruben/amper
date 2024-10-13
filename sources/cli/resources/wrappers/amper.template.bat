@echo off

@rem
@rem Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@rem

@rem Possible environment variables:
@rem   AMPER_DOWNLOAD_ROOT        Maven repository to download Amper dist from
@rem                              default: https://packages.jetbrains.team/maven/p/amper/amper
@rem   AMPER_JRE_DOWNLOAD_ROOT    Url prefix to download Amper JRE from.
@rem                              default: https:/
@rem   AMPER_BOOTSTRAP_CACHE_DIR  Cache directory to store extracted JRE and Amper distribution, must end with \
@rem   AMPER_JAVA_HOME            JRE to run Amper itself (optional, does not affect compilation)

setlocal

if defined AMPER_DOWNLOAD_ROOT (
  set amper_download_root_defined=%AMPER_DOWNLOAD_ROOT%
) else (
  set amper_download_root_defined=https://packages.jetbrains.team/maven/p/amper/amper
)

if defined AMPER_JRE_DOWNLOAD_ROOT (
  set amper_jre_download_root_defined=%AMPER_JRE_DOWNLOAD_ROOT%
) else (
  set amper_jre_download_root_defined=https:/
)

set amper_version=@AMPER_VERSION@
set amper_url=%amper_download_root_defined%/org/jetbrains/amper/cli/%amper_version%/cli-%amper_version%-dist.zip

@rem Establish chain of trust from here by specifying exact checksum of Amper distribution to be run
set amper_sha256=@AMPER_DIST_SHA256@

set jbr_version=17.0.12
set jbr_build=b1000.54
if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
    set jbr_arch=aarch64
    set jbr_sha512=9c54639b0d56235165639cf1ff75d7640d3787103819d640d18229360c3222eccc2b0f7a04faed2ee28293fa22be1080af03efc18cb78bd0380cc2de172fa8c6
) else if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
    set jbr_arch=x64
    set jbr_sha512=81e440181b30d6c587763eeb818dd933cced0c250a156773669d1652d3e848066db639c1ebec9a85792ac97286eaf111f35d6e8262758f220bc5581a159cccb2
) else (
    echo Unknown Windows architecture %PROCESSOR_ARCHITECTURE% >&2
    goto fail
)

set jbr_url=%amper_jre_download_root_defined%/cache-redirector.jetbrains.com/intellij-jbr/jbr-%jbr_version%-windows-%jbr_arch%-%jbr_build%.tar.gz
set jbr_file_name=jbr-%jbr_version%-windows-%jbr_arch%-%jbr_build%

if defined AMPER_BOOTSTRAP_CACHE_DIR goto continue_with_cache_dir
set AMPER_BOOTSTRAP_CACHE_DIR=%LOCALAPPDATA%\Amper
:continue_with_cache_dir

rem remove \ from the end if present
if [%AMPER_BOOTSTRAP_CACHE_DIR:~-1%] EQU [\] set AMPER_BOOTSTRAP_CACHE_DIR=%AMPER_BOOTSTRAP_CACHE_DIR:~0,-1%

set powershell=%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe

REM ********** Download and extract JBR **********

if defined AMPER_JAVA_HOME goto continue_with_jbr

set jbr_target_dir=%AMPER_BOOTSTRAP_CACHE_DIR%\%jbr_file_name%
call :download_and_extract "A runtime for Amper" "%jbr_url%" "%jbr_target_dir%" "%jbr_sha512%" "512"
if errorlevel 1 goto fail

set AMPER_JAVA_HOME=
for /d %%d in ("%jbr_target_dir%\*") do if exist "%%d\bin\java.exe" set AMPER_JAVA_HOME=%%d
if not exist "%AMPER_JAVA_HOME%\bin\java.exe" (
  echo Unable to find java.exe under %jbr_target_dir%
  goto fail
)

:continue_with_jbr

REM ********** Download and extract Amper **********

set amper_target_dir=%AMPER_BOOTSTRAP_CACHE_DIR%\amper-cli-%amper_version%
call :download_and_extract "The Amper %amper_version% distribution" "%amper_url%" "%amper_target_dir%" "%amper_sha256%" "256"
if errorlevel 1 goto fail

REM ********** Run Amper **********

"%AMPER_JAVA_HOME%\bin\java.exe" -ea "-Damper.wrapper.dist.sha256=%amper_sha256%" "-Damper.wrapper.process.name=%~nx0" -cp "%amper_target_dir%\lib\*" org.jetbrains.amper.cli.MainKt %*
exit /B %ERRORLEVEL%

REM ********** Download and extract any zip or .tar.gz archive **********

:download_and_extract
setlocal

set moniker=%~1
set url=%~2
set target_dir=%~3
set sha=%~4
set sha_size=%~5

set flag_file=%target_dir%\.flag
if exist "%flag_file%" (
    set /p current_flag=<"%flag_file%"
    if "%current_flag%" == "%url%" exit /b
)

@rem This multiline string is actually passed as a single line to powershell, meaning #-comments are not possible.
@rem So here are a few comments about the code below:
@rem  - we need to support both .zip and .tar.gz archives (for the Amper distribution and the JBR)
@rem  - tar should be present in all Windows machines since 2018 (and usable from both cmd and powershell)
@rem  - tar requires the destination dir to exist
@rem  - DownloadFile requires the directories in the destination file's path to exist
set download_and_extract_ps1= ^
Set-StrictMode -Version 3.0; ^
$ErrorActionPreference = 'Stop'; ^
 ^
$createdNew = $false; ^
$lock = New-Object System.Threading.Mutex($true, ('Global\amper-bootstrap.' + '%target_dir%'.GetHashCode().ToString()), [ref]$createdNew); ^
if (-not $createdNew) { ^
    Write-Host 'Waiting for the other process to finish bootstrap'; ^
    [void]$lock.WaitOne(); ^
} ^
 ^
try { ^
    if ((Get-Content '%flag_file%' -ErrorAction Ignore) -ne '%url%') { ^
        $temp_file = '%AMPER_BOOTSTRAP_CACHE_DIR%' + [System.IO.Path]::GetRandomFileName(); ^
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
        Write-Host '%moniker% will now be provisioned because this is the first run. Subsequent runs will skip this step and be faster.'; ^
        Write-Host 'Downloading %url%'; ^
        [void](New-Item '%AMPER_BOOTSTRAP_CACHE_DIR%' -ItemType Directory -Force); ^
        (New-Object Net.WebClient).DownloadFile('%url%', $temp_file); ^
 ^
        $actualSha = (Get-FileHash -Algorithm SHA%sha_size% -Path $temp_file).Hash.ToString(); ^
        if ($actualSha -ne '%sha%') { ^
          throw ('Checksum mismatch for ' + $temp_file + ' (downloaded from %url%): expected checksum %sha% but got ' + $actualSha); ^
        } ^
 ^
        Write-Host 'Extracting to %target_dir%'; ^
        if (Test-Path '%target_dir%') { ^
            Remove-Item '%target_dir%' -Recurse; ^
        } ^
        if ($temp_file -like '*.zip') { ^
            Add-Type -A 'System.IO.Compression.FileSystem'; ^
            [IO.Compression.ZipFile]::ExtractToDirectory($temp_file, '%target_dir%'); ^
        } else { ^
            [void](New-Item '%target_dir%' -ItemType Directory -Force); ^
            tar -xzf $temp_file -C '%target_dir%'; ^
        } ^
        Remove-Item $temp_file; ^
 ^
        Set-Content '%flag_file%' -Value '%url%'; ^
        Write-Host ''; ^
    } ^
} ^
finally { ^
    $lock.ReleaseMutex(); ^
}

"%powershell%" -NonInteractive -NoProfile -NoLogo -Command %download_and_extract_ps1%
if errorlevel 1 exit /b 1

exit /b 0

:fail
echo ERROR: Amper bootstrap failed, see errors above
exit /b 1
