param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Preserve vanilla animation sheets, alpha, and metadata while recoloring fluids.
# Run with PowerShell 7 on Windows against the Minecraft 1.21.1 client.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$variants = @(
    @{ Name = 'pink_potion'; Source = 'water'; Colors = @('A9678B', 'DE94B7', 'F8C9DF') },
    @{ Name = 'strange_potion'; Source = 'lava'; Colors = @('101019', '2D2D3A', '656576') },
    @{ Name = 'blue_potion'; Source = 'water'; Colors = @('306A9C', '66AEDF', 'B5E6FC') },
    @{ Name = 'silver_potion'; Source = 'water'; Colors = @('7E8590', 'BDC5CE', 'EDF1F5') }
)
$sizes = @{
    water_still = @(16, 512); water_flow = @(32, 1024)
    lava_still = @(16, 320); lava_flow = @(32, 512)
}
$outputDirectory = Join-Path $PSScriptRoot '../src/main/resources/assets/lyycore/textures/block/fluids'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)
try {
    foreach ($variant in $variants) {
        $colors = @($variant.Colors | ForEach-Object { [Drawing.ColorTranslator]::FromHtml('#' + $_) })
        foreach ($kind in @('still', 'flow')) {
            $sourceName = "$($variant.Source)_$kind"
            $sourcePath = "assets/minecraft/textures/block/$sourceName.png"
            $entry = $archive.GetEntry($sourcePath)
            $metadata = $archive.GetEntry("$sourcePath.mcmeta")
            if ($null -eq $entry -or $null -eq $metadata) { throw "Missing vanilla asset: $sourceName" }
            $stream = $entry.Open()
            try {
                $loaded = [Drawing.Bitmap]::new($stream)
                try { $bitmap = [Drawing.Bitmap]::new($loaded) }
                finally { $loaded.Dispose() }
            }
            finally { $stream.Dispose() }
            try {
                if ($bitmap.Width -ne $sizes[$sourceName][0] -or $bitmap.Height -ne $sizes[$sourceName][1]) {
                    throw "Unexpected $sourceName sheet size; use Minecraft 1.21.1."
                }
                # Shared ranges for still/flow prevent color shifts between the two.
                $minimum = if ($variant.Source -eq 'water') { 157.0 } else { 79.0 }
                $maximum = if ($variant.Source -eq 'water') { 255.0 } else { 230.0 }
                for ($y = 0; $y -lt $bitmap.Height; $y++) {
                    for ($x = 0; $x -lt $bitmap.Width; $x++) {
                        $old = $bitmap.GetPixel($x, $y)
                        $luminance = 0.2126 * $old.R + 0.7152 * $old.G + 0.0722 * $old.B
                        $t = [Math]::Clamp(($luminance - $minimum) / ($maximum - $minimum), 0.0, 1.0)
                        $segment = if ($t -lt 0.5) { 0 } else { 1 }
                        $blend = $t * 2.0 - $segment
                        $a = $colors[$segment]
                        $b = $colors[$segment + 1]
                        $r = [int][Math]::Round($a.R + ($b.R - $a.R) * $blend)
                        $g = [int][Math]::Round($a.G + ($b.G - $a.G) * $blend)
                        $blue = [int][Math]::Round($a.B + ($b.B - $a.B) * $blend)
                        $bitmap.SetPixel($x, $y, [Drawing.Color]::FromArgb($old.A, $r, $g, $blue))
                    }
                }
                $destination = Join-Path $outputDirectory "$($variant.Name)_$kind.png"
                $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
                [IO.Compression.ZipFileExtensions]::ExtractToFile($metadata, "$destination.mcmeta", $true)
                Write-Output "$($variant.Name)_${kind}: $($bitmap.Width)x$($bitmap.Height), original animation and alpha preserved."
            }
            finally { $bitmap.Dispose() }
        }
    }
}
finally { $archive.Dispose() }
