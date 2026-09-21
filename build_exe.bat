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

python -c "import flask, pymavlink, docx, openpyxl, pandas, waitress, PyInstaller" 2>NUL
if errorlevel 1 (
    echo.
    echo ERROR: one or more required packages are not installed for THIS
    echo "python" ^(the one currently on PATH^). This is the exact failure
    echo mode that produces a .exe that builds "successfully" but then
    echo crashes with "ModuleNotFoundError" when you run it -- PyInstaller
    echo bundles whatever the interpreter it runs under can see, so if the
    echo wrong Python ^(not your activated venv's^) ends up on PATH, the
    echo build silently omits packages that ARE installed, just not there.
    echo.
    echo Run this first, then re-run build_exe.bat:
    echo     pip install -r requirements.txt -r requirements-desktop.txt
    echo.
    echo If that still doesn't fix it, run "where.exe python" and confirm
    echo the first line points inside THIS project's venv\Scripts\ folder --
    echo if it doesn't, your venv isn't actually active in this terminal.
    exit /b 1
)

REM Bake the current git commit into a VERSION file bundled into the .exe,
REM so the dashboard's version footer shows which revision you built from
REM even though the packaged .exe has no .git folder inside it to check at
REM runtime (app.py falls back to reading this file when `git rev-parse`
REM isn't available, which is always the case once frozen).
set GIT_REV=
for /f "delims=" %%i in ('git rev-parse --short HEAD 2^>NUL') do set GIT_REV=%%i
if "%GIT_REV%"=="" (
    echo unknown> VERSION
    echo Could not determine a git revision ^(not a git checkout, or git not
    echo on PATH^) -- the .exe's version footer will show "unknown".
) else (
    echo %GIT_REV%> VERSION
    echo Baking in version %GIT_REV%
)

python -m PyInstaller --onefile --name NeoskyDroneAnalyzer ^
    --add-data "templates;templates" ^
    --add-data "static;static" ^
    --add-data "baselines;baselines" ^
    --add-data "VERSION;." ^
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
