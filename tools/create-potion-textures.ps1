param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftClientJar
)

# Recolor the vanilla 1.21.1 potion liquid and composite it with the glass bottle.
# The vanilla liquid mask leaves the bottle outline, stopper, and reflections intact.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Liquid shades ordered from darkest to brightest.
$shades = @('636363', '949494', '9A9A9A', 'A6A6A6', 'AAAAAA', 'C5C5C5', 'FFFFFF')
$variants = @(
    @{ Name = 'pink_potion'; Colors = @('A9678B', 'D585AC', 'DE94B7', 'E7ABC8', 'EEB7D2', 'F8C9DF', 'FFEAF4') },
    @{ Name = 'strange_potion'; Colors = @('101019', '242431', '2D2D3A', '383848', '424253', '565668', '868697') },
    @{ Name = 'blue_potion'; Colors = @('306A9C', '529AD0', '66AEDF', '7EBFE9', '95D3F4', 'B5E6FC', 'E3F8FF') },
    @{ Name = 'silver_potion'; Colors = @('7E8590', 'ABB4C0', 'BDC5CE', 'CFD6DD', 'DFE4E9', 'EDF1F5', 'FFFFFF') }
)

$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $MinecraftClientJar).Path)
function Read-Texture([string] $name) {
    $entry = $archive.GetEntry("assets/minecraft/textures/item/$name.png")
    if ($null -eq $entry) { throw "Missing vanilla texture: $name" }
    $stream = $entry.Open()
    try {
        $loaded = [Drawing.Bitmap]::new($stream)
        try { return [Drawing.Bitmap]::new($loaded) }
        finally { $loaded.Dispose() }
    }
    finally { $stream.Dispose() }
}

$glass = $null
$liquid = $null
try {
    $glass = Read-Texture 'glass_bottle'
    $liquid = Read-Texture 'potion_overlay'
    if ($glass.Width -ne 16 -or $glass.Height -ne 16 -or $liquid.Width -ne 16 -or $liquid.Height -ne 16) {
        throw 'Expected 16x16 vanilla textures.'
    }
    foreach ($variant in $variants) {
        $palette = @{}
        for ($i = 0; $i -lt $shades.Count; $i++) {
            $palette[$shades[$i]] = [Drawing.ColorTranslator]::FromHtml('#' + $variant.Colors[$i])
        }
        $bitmap = [Drawing.Bitmap]::new($glass)
        try {
            $count = 0
            for ($y = 0; $y -lt 16; $y++) {
                for ($x = 0; $x -lt 16; $x++) {
                    $old = $liquid.GetPixel($x, $y)
                    if ($old.A -eq 0) { continue }
                    if ($glass.GetPixel($x, $y).A -ne 0) { throw 'Liquid overlaps the vanilla glass.' }
                    $hex = '{0:X2}{1:X2}{2:X2}' -f $old.R, $old.G, $old.B
                    if (-not $palette.ContainsKey($hex)) { throw "Unexpected liquid color $hex; use Minecraft 1.21.1." }
                    $bitmap.SetPixel($x, $y, [Drawing.Color]::FromArgb($old.A, $palette[$hex]))
                    $count++
                }
            }
            $destination = Join-Path $PSScriptRoot "../src/main/resources/assets/lyycore/textures/item/$($variant.Name).png"
            $bitmap.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
            Write-Output "$($variant.Name): colored $count liquid pixels; preserved all glass pixels."
        }
        finally { $bitmap.Dispose() }
    }
}
finally {
    if ($null -ne $liquid) { $liquid.Dispose() }
    if ($null -ne $glass) { $glass.Dispose() }
    $archive.Dispose()
}
