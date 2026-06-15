set PGPASSWORD=303319
set PGCLIENTENCODING=UTF8
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch1.sql"
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch2_input_refs.sql"
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch3_top10.sql"
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d mili-doc -f "D:\military-doc-sandbox\backend\src\main\resources\sql\seed_data_v2_batch4_chapters.sql"
echo ALL DONE
pause
