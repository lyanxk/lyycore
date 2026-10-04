param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Add pink pixel handwriting to the vanilla Minecraft 1.21.1 paper texture.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Broken strokes follow the paper's diagonal perspective, entirely inside it.
$ink = @(
    @{ Color = '#D982AE'; Pixels = @(@(5,7), @(6,6), @(8,5), @(9,5)) },
    @{ Color = '#EDB1CE'; Pixels = @(@(7,6), @(10,5)) },
    @{ Color = '#D982AE'; Pixels = @(@(5,9), @(6,8), @(8,7), @(9,7), @(11,6)) },
    @{ Color = '#EDB1CE'; Pixels = @(@(10,6), @(12,6)) },
    @{ Color = '#D982AE'; Pixels = @(@(7,10), @(8,9), @(10,8), @(11,8)) },
    @{ Color = '#EDB1CE'; Pixels = ,@(9,9) }
)

$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)
try {
    $entry = $archive.GetEntry('assets/minecraft/textures/item/paper.png')
    if ($null -eq $entry) { throw 'Missing vanilla paper texture.' }
    $stream = $entry.Open()
    try {
        $source = [Drawing.Bitmap]::new($stream)
        try { $bitmap = [Drawing.Bitmap]::new($source) }
        finally { $source.Dispose() }
    }
    finally { $stream.Dispose() }

    try {
        if ($bitmap.Width -ne 16 -or $bitmap.Height -ne 16) { throw 'Expected 16x16 paper.' }
        $changed = 0
        foreach ($stroke in $ink) {
            $color = [Drawing.ColorTranslator]::FromHtml($stroke.Color)
            foreach ($pixel in $stroke.Pixels) {
                $x, $y = $pixel
                $old = $bitmap.GetPixel($x, $y)
                if ($old.A -ne 255 -or $old.R -ne 252 -or $old.G -ne 252 -or $old.B -ne 242) {
                    throw "Ink at ($x, $y) must be inside the vanilla paper surface."
                }
                $bitmap.SetPixel($x, $y, $color)
                $changed++
            }
        }
        $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/item/research_notes.png'
        $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
        Write-Output "research_notes: added $changed pink ink pixels; preserved the vanilla paper and alpha elsewhere."
    }
    finally { $bitmap.Dispose() }
}
finally { $archive.Dispose() }
