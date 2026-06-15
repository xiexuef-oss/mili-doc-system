$env:PGPASSWORD='***'
...sql='C:\Program Files\PostgreSQL\18\bin\psql.exe'
$dir='D:\military-doc-sandbox\backend\src\main\resources\sql'
$files=@('seed_data_v2_batch1.sql','seed_data_v2_batch2_input_refs.sql','seed_data_v2_batch3_top10.sql','seed_data_v2_batch4_chapters.sql')
foreach ($f in $files) {
    Write-Host "=== $f ==="
    iex "$psql -U postgres -d mili-doc -f `"$dir\$f`""
}
Write-Host "DONE"
