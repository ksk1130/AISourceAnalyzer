@echo off
setlocal

REM C:\bin が存在しなければ作成
if not exist "C:\bin" (
    mkdir "C:\bin"
)

REM JDKのダウンロード
set JDK_ZIP=C:\bin\amazon-corretto-21-x64-windows-jdk.zip
powershell -Command "Invoke-WebRequest -Uri 'https://corretto.aws/downloads/latest/amazon-corretto-21-x64-windows-jdk.zip' -OutFile '%JDK_ZIP%'"

REM 既存の展開先があれば削除
if exist "C:\bin\jdk-21" (
    rmdir /s /q "C:\bin\jdk-21"
)

REM zipを展開（PowerShell使用）
powershell -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath 'C:\bin\jdk-21-tmp'"

REM サブディレクトリ名を取得してjdk-21にリネーム
for /d %%D in (C:\bin\jdk-21-tmp\*) do (
    move "%%D" "C:\bin\jdk-21"
)
rmdir /s /q "C:\bin\jdk-21-tmp"

REM 完了メッセージ
echo JDK展開完了: C:\bin\jdk-21\bin\java

endlocal