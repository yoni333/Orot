Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$src = Get-ChildItem -Path 'docs' -Filter '*.jpg' | Select-Object -First 1
$img = [System.Drawing.Image]::FromFile($src.FullName)

# Artwork bounds measured from the ink-density profile, plus a little air.
$PAD = 18
$AX = 444 - $PAD; $AY = 156 - $PAD
$AW = 1300 + 2*$PAD; $AH = 1764 + 2*$PAD

$PAPER = [System.Drawing.ColorTranslator]::FromHtml('#FBF9F1')  # BackgroundLight
$INK_R = 0x1C/255.0; $INK_G = 0x1C/255.0; $INK_B = 0x17/255.0   # OnBackgroundLight

# --- Ink master: luminance -> alpha, RGB forced to the ink colour. -------------
# alpha = (1 - lum) * 1.18 - 0.035, so JPEG paper noise (lum > ~0.97) lands on 0
# and the drawing keeps its anti-aliased edges instead of being hard-thresholded.
$cm = New-Object System.Drawing.Imaging.ColorMatrix
$cm.Matrix00 = 0; $cm.Matrix01 = 0; $cm.Matrix02 = 0; $cm.Matrix03 = -0.35282
$cm.Matrix10 = 0; $cm.Matrix11 = 0; $cm.Matrix12 = 0; $cm.Matrix13 = -0.69266
$cm.Matrix20 = 0; $cm.Matrix21 = 0; $cm.Matrix22 = 0; $cm.Matrix23 = -0.13452
$cm.Matrix30 = 0; $cm.Matrix31 = 0; $cm.Matrix32 = 0; $cm.Matrix33 = 0
$cm.Matrix40 = $INK_R; $cm.Matrix41 = $INK_G; $cm.Matrix42 = $INK_B; $cm.Matrix43 = 1.145
$ia = New-Object System.Drawing.Imaging.ImageAttributes
$ia.SetColorMatrix($cm)

$MH = 1400
$MW = [int][math]::Round($MH * $AW / $AH)
$ink = New-Object System.Drawing.Bitmap -ArgumentList $MW, $MH, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gi = [System.Drawing.Graphics]::FromImage($ink)
$gi.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gi.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gi.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$gi.Clear([System.Drawing.Color]::Transparent)
$dst = New-Object System.Drawing.Rectangle -ArgumentList 0, 0, $MW, $MH
$gi.DrawImage($img, $dst, $AX, $AY, $AW, $AH, [System.Drawing.GraphicsUnit]::Pixel, $ia)
$gi.Dispose()
Write-Output ("ink master: {0}x{1}" -f $MW, $MH)

function New-Icon {
  param([string]$Path, [int]$Size, [double]$Frac, [string]$Bg)
  $b = New-Object System.Drawing.Bitmap -ArgumentList $Size, $Size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($b)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.Clear([System.Drawing.Color]::Transparent)
  if ($Bg -eq 'square') { $g.Clear($PAPER) }
  elseif ($Bg -eq 'circle') {
    $br = New-Object System.Drawing.SolidBrush -ArgumentList $PAPER
    $g.FillEllipse($br, 0, 0, $Size - 1, $Size - 1); $br.Dispose()
    $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
    $gp.AddEllipse(0, 0, $Size - 1, $Size - 1); $g.SetClip($gp); $gp.Dispose()
  }
  $h = $Size * $Frac
  $w = $h * $MW / $MH
  $g.DrawImage($ink, [float](($Size - $w)/2.0), [float](($Size - $h)/2.0), [float]$w, [float]$h)
  $g.Dispose()
  $dir = Split-Path $Path -Parent
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
  $b.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
  Write-Output ("  {0}  ({1}x{1})" -f $Path, $Size)
}

$dens = @{ 'mdpi'=1.0; 'hdpi'=1.5; 'xhdpi'=2.0; 'xxhdpi'=3.0; 'xxxhdpi'=4.0 }

Write-Output "Play store icon:"
New-Icon -Path 'docs/icon/play-icon-512.png' -Size 512 -Frac 0.90 -Bg 'square'

Write-Output "Adaptive foreground (108dp canvas, art inside the 72dp safe zone):"
foreach ($d in $dens.Keys) {
  New-Icon -Path ("app/src/main/res/drawable-{0}/ic_launcher_foreground.png" -f $d) -Size ([int](108*$dens[$d])) -Frac 0.64 -Bg 'none'
}

Write-Output "Legacy launcher rasters (API 24-25):"
foreach ($d in $dens.Keys) {
  New-Icon -Path ("app/src/main/res/mipmap-{0}/ic_launcher.png" -f $d)       -Size ([int](48*$dens[$d])) -Frac 0.86 -Bg 'square'
  New-Icon -Path ("app/src/main/res/mipmap-{0}/ic_launcher_round.png" -f $d) -Size ([int](48*$dens[$d])) -Frac 0.68 -Bg 'circle'
}

# --- Previews: what this actually looks like at real launcher size -------------
function New-Preview {
  param([string]$Path, [int]$Real, [int]$Zoom, [switch]$CircleMask)
  $b = New-Object System.Drawing.Bitmap -ArgumentList $Real, $Real, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($b)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.Clear([System.Drawing.Color]::White)
  # simulate the adaptive icon: 108dp canvas, circle mask at 72dp, art at 0.64
  $canvas = $Real * 108.0 / 72.0
  $off = -($canvas - $Real) / 2.0
  $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
  $gp.AddEllipse(0, 0, $Real - 1, $Real - 1); $g.SetClip($gp)
  $br = New-Object System.Drawing.SolidBrush -ArgumentList $PAPER
  $g.FillRectangle($br, $off, $off, $canvas, $canvas); $br.Dispose()
  $h = $canvas * 0.64; $w = $h * $MW / $MH
  $g.DrawImage($ink, [float]($off + ($canvas - $w)/2.0), [float]($off + ($canvas - $h)/2.0), [float]$w, [float]$h)
  $g.Dispose(); $gp.Dispose()
  $z = New-Object System.Drawing.Bitmap -ArgumentList ($Real*$Zoom), ($Real*$Zoom), ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gz = [System.Drawing.Graphics]::FromImage($z)
  $gz.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $gz.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $gz.DrawImage($b, 0, 0, $Real*$Zoom, $Real*$Zoom)
  $gz.Dispose()
  $z.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png); $z.Dispose(); $b.Dispose()
  Write-Output ("  preview {0}: real {1}px shown at {2}x" -f $Path, $Real, $Zoom)
}
Write-Output "Previews:"
New-Preview -Path ($env:SCRATCH + '/preview-48.png')  -Real 48  -Zoom 8
New-Preview -Path ($env:SCRATCH + '/preview-72.png')  -Real 72  -Zoom 5
New-Preview -Path ($env:SCRATCH + '/preview-192.png') -Real 192 -Zoom 2

$ink.Dispose(); $img.Dispose()
Write-Output "DONE"
