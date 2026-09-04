@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=ar.edu.um.ingenieria.limitador.bulk.BulkUserCsvGenerator"
endlocal