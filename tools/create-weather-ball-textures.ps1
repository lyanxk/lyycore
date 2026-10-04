param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Derive two 16x16 weather icons from the vanilla Minecraft 1.21.1 snowball.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$variants = @(
    @{
        Name = 'storm_ball'
        Palette = @{
            '7BA6A6' = '36566F'; 'AFCACA' = '557C99'; 'C2DADA' = '739DBC'
            'D0F1F1' = '93C5DD'; 'E8F8F8' = 'BBDDEB'; 'FFFFFF' = 'DBF4FF'
        }
        Marks = @(
            @{ Color = '#3282B5'; Pixels = @(@(6,7), @(5,8), @(9,7), @(8,8), @(10,10), @(9,11)) },
            @{ Color = '#67C6F0'; Pixels = @(@(6,6), @(9,6), @(10,9)) }
        )
    },
    @{
        Name = 'sun_ball'
        Palette = @{
            '7BA6A6' = 'A95021'; 'AFCACA' = 'C98531'; 'C2DADA' = 'F2B84B'
            'D0F1F1' = 'FFD26A'; 'E8F8F8' = 'FFE3A0'; 'FFFFFF' = 'FFF4CD'
        }
        Marks = @(
            @{ Color = '#FFFBE3'; Pixels = @(@(7,7), @(8,7), @(7,8), @(8,8)) },
            @{ Color = '#EBA02D'; Pixels = @(@(7,5), @(8,5), @(7,10), @(8,10), @(5,7), @(5,8), @(10,7), @(10,8), @(6,6), @(9,6), @(6,9), @(9,9)) }
        )
    }
)

$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)
try {
    $entry = $archive.GetEntry('assets/minecraft/textures/item/snowball.png')
    if ($null -eq $entry) { throw 'Missing vanilla snowball texture.' }
    $stream = $entry.Open()
    try {
        $loaded = [Drawing.Bitmap]::new($stream)
        try { $source = [Drawing.Bitmap]::new($loaded) }
        finally { $loaded.Dispose() }
    }
    finally { $stream.Dispose() }

    try {
        if ($source.Width -ne 16 -or $source.Height -ne 16) { throw 'Expected 16x16 snowball.' }
        foreach ($variant in $variants) {
            $bitmap = [Drawing.Bitmap]::new($source)
            try {
                for ($y = 0; $y -lt 16; $y++) {
                    for ($x = 0; $x -lt 16; $x++) {
                        $old = $source.GetPixel($x, $y)
                        if ($old.A -eq 0) { continue }
                        $hex = '{0:X2}{1:X2}{2:X2}' -f $old.R, $old.G, $old.B
                        if (-not $variant.Palette.ContainsKey($hex)) {
                            throw "Unexpected color $hex; use the Minecraft 1.21.1 client."
                        }
                        $color = [Drawing.ColorTranslator]::FromHtml('#' + $variant.Palette[$hex])
                        $bitmap.SetPixel($x, $y, [Drawing.Color]::FromArgb($old.A, $color))
                    }
                }
                foreach ($mark in $variant.Marks) {
                    $color = [Drawing.ColorTranslator]::FromHtml($mark.Color)
                    foreach ($pixel in $mark.Pixels) {
                        $x, $y = $pixel
                        if ($source.GetPixel($x, $y).A -ne 255) { throw 'Mark falls outside the snowball.' }
                        $bitmap.SetPixel($x, $y, $color)
                    }
                }
                $destination = Join-Path $PSScriptRoot "../src/main/resources/assets/lyycore/textures/item/$($variant.Name).png"
                $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
                Write-Output "$($variant.Name): recolored vanilla snowball with weather markings."
            }
            finally { $bitmap.Dispose() }
        }
    }
    finally { $source.Dispose() }
}
finally { $archive.Dispose() }
