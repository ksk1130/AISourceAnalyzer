@echo off
setlocal

REM C:\bin �����݂��Ȃ���΍쐬
if not exist "C:\bin" (
    mkdir "C:\bin"
)

REM JDK�̃_�E�����[�h
set JDK_ZIP=C:\bin\amazon-corretto-21-x64-windows-jdk.zip
powershell -Command "Invoke-WebRequest -Uri 'https://corretto.aws/downloads/latest/amazon-corretto-21-x64-windows-jdk.zip' -OutFile '%JDK_ZIP%'"

REM �����̓W�J�悪����΍폜
if exist "C:\bin\jdk-21" (
    rmdir /s /q "C:\bin\jdk-21"
)

REM zip��W�J�iPowerShell�g�p�j
powershell -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath 'C:\bin\jdk-21-tmp'"

REM �T�u�f�B���N�g�������擾����jdk-21�Ƀ��l�[��
for /d %%D in (C:\bin\jdk-21-tmp\*) do (
    move "%%D" "C:\bin\jdk-21"
)
rmdir /s /q "C:\bin\jdk-21-tmp"

REM �������b�Z�[�W
echo JDK�W�J����: C:\bin\jdk-21\bin\java

endlocal