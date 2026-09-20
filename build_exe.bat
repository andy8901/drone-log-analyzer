@echo off
REM Builds the standalone Windows .exe. Run this ON WINDOWS from the repo
REM root, with your virtualenv activated and both requirements files
REM installed (see README.md "Desktop app (.exe)" for full setup steps).
REM
REM Uses "python -m PyInstaller" rather than the bare "pyinstaller" command:
REM the latter depends on your venv's Scripts\ folder being on PATH, which
REM isn't always reliable on Windows even with the venv activated, and fails
REM with a plain "not recognized" error that's easy to mistake for a real
REM build failure. "python -m PyInstaller" instead goes through the same
REM Python interpreter already resolved on PATH, so it works whenever
REM PyInstaller is installed for that interpreter, period.

python -c "import PyInstaller" 2>NUL
if errorlevel 1 (
    echo.
    echo ERROR: PyInstaller is not installed for this Python interpreter.
    echo Run this first, then re-run build_exe.bat:
    echo     pip install -r requirements.txt -r requirements-desktop.txt
    exit /b 1
)

python -m PyInstaller --onefile --name NeoskyDroneAnalyzer ^
    --add-data "templates;templates" ^
    --add-data "static;static" ^
    --add-data "baselines;baselines" ^
    --hidden-import=pymavlink.dialects.v20.ardupilotmega ^
    --hidden-import=pymavlink.dialects.v10.ardupilotmega ^
    --collect-submodules pymavlink ^
    desktop_launcher.py

if errorlevel 1 (
    echo.
    echo BUILD FAILED. Scroll up for the actual PyInstaller error -- common
    echo causes are a missing dependency ^(re-run the pip install above^) or
    echo antivirus quarantining a file mid-build.
    exit /b 1
)

if not exist "dist\NeoskyDroneAnalyzer.exe" (
    echo.
    echo PyInstaller reported success but dist\NeoskyDroneAnalyzer.exe is
    echo missing. Something unexpected happened -- scroll up for details.
    exit /b 1
)

echo.
echo Build finished. The .exe is in dist\NeoskyDroneAnalyzer.exe
echo Copy it anywhere you like -- it creates its own uploads/reports_out/
echo sessions/archive folders next to wherever it's run from.
