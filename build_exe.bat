@echo off
REM Builds the standalone Windows .exe. Run this ON WINDOWS from the repo
REM root, with your virtualenv activated and both requirements files
REM installed (see README.md "Desktop app (.exe)" for full setup steps).

pyinstaller --onefile --name NeoskyDroneAnalyzer ^
    --add-data "templates;templates" ^
    --add-data "static;static" ^
    --add-data "baselines;baselines" ^
    --hidden-import=pymavlink.dialects.v20.ardupilotmega ^
    --hidden-import=pymavlink.dialects.v10.ardupilotmega ^
    --collect-submodules pymavlink ^
    --icon=NONE ^
    desktop_launcher.py

echo.
echo Build finished. The .exe is in dist\NeoskyDroneAnalyzer.exe
echo Copy it anywhere you like -- it creates its own uploads/reports_out/
echo sessions/archive folders next to wherever it's run from.
