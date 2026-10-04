$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# 16x16 handheld coordinate reader: white/pink casing, screen crosshair,
# coordinate readout, buttons, and a short antenna.
$palette = @{
    O = '#625D78' # outline
    S = '#A8A3BA' # casing shadow
    W = '#E5E6F0' # casing
    H = '#FFFFFF' # highlight
    P = '#E8ABCB' # pink trim
    D = '#B56896' # button shadow
    B = '#293E59' # screen
    G = '#395A70' # screen grid
    C = '#9FDDEC' # coordinate readout
    X = '#FFE2F2' # target crosshair
}
$rows = @(
    '................',
    '...........OO...',
    '...........SO...',
    '....OOOOOOOOO...',
    '...OHWWWWWWWSO..',
    '...OWOBBGBBOWO..',
    '...OWOBBXBBOWO..',
    '...OWOGXXXGOWO..',
    '...OWOBBXBBOWO..',
    '...OWOBBGBBOWO..',
    '...OWOCCBCCOWO..',
    '...OWOOOOOOWSO..',
    '...OWHPWPDDPSO..',
    '...OSPPPPPPSSO..',
    '....OOOOOOOOO...',
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
            $bitmap.SetPixel($x, $y, [Drawing.ColorTranslator]::FromHtml($palette[$symbol]))
        }
    }
    $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/item/coordinate_device.png'
    $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
    Write-Output 'coordinate_device: created 16x16 coordinate reader with transparent background.'
}
finally { $bitmap.Dispose() }
