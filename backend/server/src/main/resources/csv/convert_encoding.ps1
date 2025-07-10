$files = Get-ChildItem -Path . -Filter *.csv
foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Encoding Default
    $content | Out-File -FilePath $file.FullName -Encoding utf8  # �� -FilePath ��� -Path
    Write-Host "ת����ɣ�$($file.Name)"
}