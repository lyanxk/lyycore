param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Rebuild the 1.21.1 ore textures from the vanilla client assets.
# Run with PowerShell 7 on Windows; no image libraries need installing.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$outputDirectory = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/block'
$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)

function Read-Texture([string] $name) {
    $entry = $archive.GetEntry("assets/minecraft/textures/block/$name.png")
    if ($null -eq $entry) { throw "Missing vanilla texture: $name" }
    $stream = $entry.Open()
    try {
        $loaded = [Drawing.Bitmap]::new($stream)
        try { return [Drawing.Bitmap]::new($loaded) }
        finally { $loaded.Dispose() }
    }
    finally { $stream.Dispose() }
}

function Get-Hex([Drawing.Color] $color) {
    return '{0:X2}{1:X2}{2:X2}' -f $color.R, $color.G, $color.B
}

# Explicit palettes include the mineral bevels and leave the rock unchanged.
# Deepslate has additional mineral shadows, plus blue-gray rock colors that
# must be preserved; do not select every non-gray pixel indiscriminately.
$stonePalette = @{
    '239698' = 'A34D85'
    '1ED0D6' = 'EB75B9'
    '77E7D1' = 'F7AED6'
    'D5FFF6' = 'FFE5F4'
    '8DADB1' = 'B19AAA'
}
$deepPalette = @{
    '148F92' = '8F3977'
    '1ED0D6' = 'EB75B9'
    '77E7D1' = 'F7AED6'
    'D5FFF6' = 'FFE5F4'
    '648A8C' = '8C7184'
    '617977' = '796575'
    '314040' = '40313B'
}

try {
    foreach ($variant in @(
        @{ Source = 'diamond_ore'; Target = 'imaginium_ore'; Palette = $stonePalette; Count = 57 },
        @{ Source = 'deepslate_diamond_ore'; Target = 'deepslate_imaginium_ore'; Palette = $deepPalette; Count = 86 }
    )) {
        $bitmap = Read-Texture $variant.Source
        try {
            if ($bitmap.Width -ne 16 -or $bitmap.Height -ne 16) { throw 'Expected 16x16 texture.' }
            $changed = 0
            for ($y = 0; $y -lt 16; $y++) {
                for ($x = 0; $x -lt 16; $x++) {
                    $old = $bitmap.GetPixel($x, $y)
                    $hex = Get-Hex $old
                    if (-not $variant.Palette.ContainsKey($hex)) { continue }
                    $rgb = [Drawing.ColorTranslator]::FromHtml('#' + $variant.Palette[$hex])
                    $bitmap.SetPixel($x, $y, [Drawing.Color]::FromArgb($old.A, $rgb))
                    $changed++
                }
            }
            if ($changed -ne $variant.Count) {
                throw 'Unexpected vanilla ore mask; use the Minecraft 1.21.1 client.'
            }
            $destination = Join-Path $outputDirectory ($variant.Target + '.png')
            $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
            Write-Output "$($variant.Target): recolored $changed mineral pixels; preserved $(256 - $changed) rock pixels."
        }
        finally { $bitmap.Dispose() }
    }
}
finally {
    $archive.Dispose()
}
