@echo off
set PGPASSWORD=303319

echo [1/4] seed_data_v2_batch1.sql
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch1.sql"
if errorlevel 1 goto :err

echo [2/4] seed_data_v2_batch2_input_refs.sql
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch2_input_refs.sql"
if errorlevel 1 goto :err

echo [3/4] seed_data_v2_batch3_top10.sql
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch3_top10.sql"
if errorlevel 1 goto :err

echo [4/4] seed_data_v2_batch4_chapters.sql
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch4_chapters.sql"
if errorlevel 1 goto :err

echo ALL DONE
pause
goto :end

:err
echo FAILED
pause

:end
