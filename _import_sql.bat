@echo off
set PGPASSWORD=*** UTF8
set PSQL="C:\Program Files\PostgreSQL\18\bin\psql.exe"

echo [1/4] Template chapters...
%PSQL% -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch1.sql"
if errorlevel 1 echo --- BATCH1 ERRORS ---

echo [2/4] Input references...
%PSQL% -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch2_input_refs.sql"

echo [3/4] Top 10 docs...
%PSQL% -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch3_top10.sql"

echo [4/4] B/C spec chapters...
%PSQL% -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch4_chapters.sql"

echo ALL DONE
pause
