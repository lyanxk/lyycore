param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Recolor the vanilla 1.21.1 netherite ingot without changing its pixel layout.
# Run with PowerShell 7 on Windows.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Silver-white top, pale pink sides, and lavender shadows from the old ingot.
$palette = @{
    '111111' = '726F94'
    '271C1D' = 'ADA8BD'
    '31292A' = 'D49FB8'
    '3B393B' = 'AEA8BE'
    '3C3232' = 'E9C3D4'
    '484548' = 'D0D5E2'
    '4D494D' = 'EAEEF9'
    '4C4143' = 'FCE8F2'
    '5A575A' = 'FCFCFC'
    '737173' = 'FEFEFE'
}

$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)
try {
    $entry = $archive.GetEntry('assets/minecraft/textures/item/netherite_ingot.png')
    if ($null -eq $entry) { throw 'Missing vanilla netherite ingot texture.' }
    $stream = $entry.Open()
    try {
        $source = [Drawing.Bitmap]::new($stream)
        try {
            $bitmap = [Drawing.Bitmap]::new($source)
        }
        finally { $source.Dispose() }
    }
    finally { $stream.Dispose() }

    try {
        if ($bitmap.Width -ne 16 -or $bitmap.Height -ne 16) { throw 'Expected 16x16 texture.' }
        $changed = 0
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $old = $bitmap.GetPixel($x, $y)
                if ($old.A -eq 0) { continue }
                $hex = '{0:X2}{1:X2}{2:X2}' -f $old.R, $old.G, $old.B
                if (-not $palette.ContainsKey($hex)) {
                    throw "Unexpected vanilla color $hex; use the Minecraft 1.21.1 client."
                }
                $rgb = [Drawing.ColorTranslator]::FromHtml('#' + $palette[$hex])
                $bitmap.SetPixel($x, $y, [Drawing.Color]::FromArgb($old.A, $rgb))
                $changed++
            }
        }
        if ($changed -ne 135) { throw 'Unexpected vanilla ingot mask; use the Minecraft 1.21.1 client.' }
        $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/item/imaginary_alloy_ingot.png'
        $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
        Write-Output "imaginary_alloy_ingot: recolored $changed pixels; preserved the vanilla silhouette and alpha."
    }
    finally { $bitmap.Dispose() }
}
finally { $archive.Dispose() }
