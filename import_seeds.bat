@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM 种子数据导入脚本 — 先执行 migration 建表，再导入种子数据
REM 使用方法:
REM   1. 确保 psql 在 PATH 中, 或设置 PG_BIN 环境变量
REM   2. import_seeds.bat
REM ============================================================

set PGPASSWORD=303319
set PG_DB=mili-doc
set PG_USER=postgres
set SQL_DIR=D:\military-doc-sandbox\backend\src\main\resources\sql

REM 尝试多种方式找到 psql
if defined PG_BIN (
    set PSQL=%PG_BIN%\psql.exe
) else (
    set PSQL=psql.exe
)

echo [CHECK] 检查 psql 可用性...
"%PSQL%" --version >nul 2>&1
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] 找不到 psql。请安装 PostgreSQL 或将 PG_BIN 设置为 psql.exe 所在目录
    echo         例如: set PG_BIN=C:\Program Files\PostgreSQL\14\bin
    pause
    exit /b 1
)
echo [OK] psql 就绪

REM ========================================
REM Phase 1: 执行 migration 脚本
REM ========================================
echo.
echo ========================================
echo Phase 1: 执行数据库迁移 (CREATE TABLE)
echo ========================================

set MIGRATIONS[0]=migration_v2_chapter_structure.sql
set MIGRATIONS[1]=migration_v3_library_fusion.sql
set MIGRATIONS[2]=migration_v4_constraints_indexes.sql
set MIGRATIONS[3]=migration_v5_stage_doc_checklist.sql
set MIGRATIONS[4]=migration_v6_doc_input_reference.sql

set MIGRATION_FAILED=0
for /l %%i in (0,1,4) do (
    echo [MIG] Running !MIGRATIONS[%%i]! ...
    "%PSQL%" -U %PG_USER% -d %PG_DB% -f "%SQL_DIR%\!MIGRATIONS[%%i]!" -q
    if !ERRORLEVEL! NEQ 0 (
        echo [FAIL] !MIGRATIONS[%%i]! 执行失败!
        set MIGRATION_FAILED=1
    ) else (
        echo [OK]   !MIGRATIONS[%%i]! 完成
    )
)

if %MIGRATION_FAILED% EQU 1 (
    echo.
    echo [ABORT] Migration 阶段有失败，终止导入
    pause
    exit /b 1
)

REM ========================================
REM Phase 2: 执行种子数据脚本
REM ========================================
echo.
echo ========================================
echo Phase 2: 导入种子数据
echo ========================================

set SEEDS[0]=seed_data_v2_batch1.sql
set SEEDS[1]=seed_data_v2_batch2_input_refs.sql
set SEEDS[2]=seed_data_v2_batch3_top10.sql
set SEEDS[3]=seed_data_v2_batch4_chapters.sql

set SEED_FAILED=0
for /l %%i in (0,1,3) do (
    echo [SEED] Running !SEEDS[%%i]! ...
    "%PSQL%" -U %PG_USER% -d %PG_DB% -f "%SQL_DIR%\!SEEDS[%%i]!" -q
    if !ERRORLEVEL! NEQ 0 (
        echo [FAIL] !SEEDS[%%i]! 执行失败!
        set SEED_FAILED=1
    ) else (
        echo [OK]   !SEEDS[%%i]! 完成
    )
)

echo.
if %SEED_FAILED% EQU 1 (
    echo [WARN] 部分种子数据导入失败，请检查上面的错误信息
) else (
    echo [DONE] 所有迁移和种子数据导入完成!
)
pause
