$ErrorActionPreference='Stop'
$project=Split-Path -Parent $PSScriptRoot
$exe=Join-Path $project '.tools/mysql/bin/mysqld.exe'
$ini=Join-Path $project '.runtime/mysql/my.ini'
if(!(Test-Path -LiteralPath $exe) -or !(Test-Path -LiteralPath $ini)){Write-Output 'Local MySQL is not installed; skipping.';exit 0}
function Test-LocalDb {
    $client=[Net.Sockets.TcpClient]::new()
    try{$task=$client.ConnectAsync('127.0.0.1',3306);return ($task.Wait(300) -and $client.Connected)}catch{return $false}finally{$client.Dispose()}
}
if(Test-LocalDb){exit 0}
Start-Process -FilePath $exe -ArgumentList "--defaults-file=`"$ini`"",'--console' -WorkingDirectory $project -WindowStyle Hidden -RedirectStandardOutput (Join-Path $project '.runtime/mysql/stdout.log') -RedirectStandardError (Join-Path $project '.runtime/mysql/stderr.log')
for($attempt=0;$attempt -lt 40;$attempt++){if(Test-LocalDb){Write-Output 'Local MySQL ready.';exit 0};Start-Sleep -Milliseconds 250}
throw 'Local MySQL did not start. Check .runtime/mysql/stderr.log'
