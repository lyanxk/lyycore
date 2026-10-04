param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Draw a gold lightning emblem inside the vanilla Minecraft 1.21.1 glass bottle.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Keep the neck, outline, and glass reflections untouched.
$ink = @(
    @{ Color = '#FFDF4F'; Pixels = @(@(10,8), @(9,9), @(7,10), @(8,10), @(9,10), @(8,12), @(7,13)) },
    @{ Color = '#FFF19A'; Pixels = @(@(9,8), @(8,9)) },
    @{ Color = '#EBA62B'; Pixels = @(@(10,10), @(9,11)) }
)

$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)
try {
    $entry = $archive.GetEntry('assets/minecraft/textures/item/glass_bottle.png')
    if ($null -eq $entry) { throw 'Missing vanilla glass bottle texture.' }
    $stream = $entry.Open()
    try {
        $source = [Drawing.Bitmap]::new($stream)
        try { $bitmap = [Drawing.Bitmap]::new($source) }
        finally { $source.Dispose() }
    }
    finally { $stream.Dispose() }

    try {
        if ($bitmap.Width -ne 16 -or $bitmap.Height -ne 16) { throw 'Expected 16x16 glass bottle.' }
        $changed = 0
        foreach ($stroke in $ink) {
            $color = [Drawing.ColorTranslator]::FromHtml($stroke.Color)
            foreach ($pixel in $stroke.Pixels) {
                $x, $y = $pixel
                if ($bitmap.GetPixel($x, $y).A -ne 0) {
                    throw "Lightning at ($x, $y) would overwrite the vanilla glass."
                }
                $bitmap.SetPixel($x, $y, $color)
                $changed++
            }
        }
        $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/item/lightning_bottle.png'
        $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
        Write-Output "lightning_bottle: added $changed lightning pixels; preserved all vanilla glass pixels."
    }
    finally { $bitmap.Dispose() }
}
finally { $archive.Dispose() }
