$files = Get-ChildItem -Path . -Filter *.csv
foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Encoding Default
    $content | Out-File -FilePath $file.FullName -Encoding utf8  # 用 -FilePath 替代 -Path
    Write-Host "转换完成：$($file.Name)"
}