$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# 16x16 item sprite: open glass test tube containing pale blue solution.
# '.' is transparent; the empty glass interior is translucent.
$palette = @{
    O = [Drawing.ColorTranslator]::FromHtml('#597F9B')
    G = [Drawing.ColorTranslator]::FromHtml('#9DBED3')
    H = [Drawing.ColorTranslator]::FromHtml('#EFFBFF')
    T = [Drawing.Color]::FromArgb(65, 193, 224, 239)
    M = [Drawing.ColorTranslator]::FromHtml('#D5F5FF')
    L = [Drawing.ColorTranslator]::FromHtml('#A8E0F5')
    S = [Drawing.ColorTranslator]::FromHtml('#79BBD9')
    B = [Drawing.ColorTranslator]::FromHtml('#E5F9FF')
}
$rows = @(
    '................',
    '.....OOOOOO.....',
    '....OHHHGGGO....',
    '.....OGTTGO.....',
    '.....OHTTTO.....',
    '.....OHTGGO.....',
    '.....OHTTTO.....',
    '.....OHMMMO.....',
    '.....OHLLSO.....',
    '.....OHBLSO.....',
    '.....OHLLSO.....',
    '.....OHLLBO.....',
    '.....OGLLSO.....',
    '......OGSO......',
    '.......OO.......',
    '................'
)

$bitmap = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    if ($rows.Count -ne 16) { throw 'Expected 16 rows.' }
    for ($y = 0; $y -lt 16; $y++) {
        if ($rows[$y].Length -ne 16) { throw "Expected 16 pixels in row $y." }
        for ($x = 0; $x -lt 16; $x++) {
            $symbol = [string]$rows[$y][$x]
            if ($symbol -eq '.') { continue }
            if (-not $palette.ContainsKey($symbol)) { throw "Unknown palette symbol: $symbol" }
            $bitmap.SetPixel($x, $y, $palette[$symbol])
        }
    }
    $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/item/supermutation_factor.png'
    $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
    Write-Output 'supermutation_factor: created 16x16 glass test tube with pale blue solution.'
}
finally { $bitmap.Dispose() }
