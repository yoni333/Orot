Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

# Rendered at 2x and downsampled, so the Hebrew type keeps clean edges.
$W = 2048; $H = 1000; $S = 2

$SAGE_TOP = [System.Drawing.ColorTranslator]::FromHtml('#6A705A')
$SAGE_BOT = [System.Drawing.ColorTranslator]::FromHtml('#434835')
$PAPER    = [System.Drawing.ColorTranslator]::FromHtml('#FBF9F1')
$CREAM    = [System.Drawing.ColorTranslator]::FromHtml('#FBF9F1')
$MUTED    = [System.Drawing.ColorTranslator]::FromHtml('#DDE2C6')
$FAINT    = [System.Drawing.ColorTranslator]::FromHtml('#C2C7AC')
$INK_R = 0x1C/255.0; $INK_G = 0x1C/255.0; $INK_B = 0x17/255.0

$fams = (New-Object System.Drawing.Text.InstalledFontCollection).Families | ForEach-Object { $_.Name }
function Pick-Font { param([int]$Px, [System.Drawing.FontStyle]$Style)
  foreach ($n in 'Rubik','Segoe UI','Arial') {
    if ($fams -contains $n) {
      try { return (New-Object System.Drawing.Font -ArgumentList $n, ([float]$Px), $Style, ([System.Drawing.GraphicsUnit]::Pixel)) } catch { }
    }
  }
  throw 'no usable font'
}

# ---- ink master from the source sketch (same extraction as the icon) ----------
$src = Get-ChildItem -Path 'docs' -Filter '*.jpg' | Select-Object -First 1
$img = [System.Drawing.Image]::FromFile($src.FullName)
$PAD = 18; $AX = 444-$PAD; $AY = 156-$PAD; $AW = 1300+2*$PAD; $AH = 1764+2*$PAD
$cm = New-Object System.Drawing.Imaging.ColorMatrix
$cm.Matrix00=0;$cm.Matrix01=0;$cm.Matrix02=0;$cm.Matrix03=-0.35282
$cm.Matrix10=0;$cm.Matrix11=0;$cm.Matrix12=0;$cm.Matrix13=-0.69266
$cm.Matrix20=0;$cm.Matrix21=0;$cm.Matrix22=0;$cm.Matrix23=-0.13452
$cm.Matrix30=0;$cm.Matrix31=0;$cm.Matrix32=0;$cm.Matrix33=0
$cm.Matrix40=$INK_R;$cm.Matrix41=$INK_G;$cm.Matrix42=$INK_B;$cm.Matrix43=1.145
$ia = New-Object System.Drawing.Imaging.ImageAttributes; $ia.SetColorMatrix($cm)
$MH = 1400; $MW = [int][math]::Round($MH*$AW/$AH)
$ink = New-Object System.Drawing.Bitmap -ArgumentList $MW,$MH,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gi = [System.Drawing.Graphics]::FromImage($ink)
$gi.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gi.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gi.Clear([System.Drawing.Color]::Transparent)
$rc = New-Object System.Drawing.Rectangle -ArgumentList 0,0,$MW,$MH
$gi.DrawImage($img,$rc,$AX,$AY,$AW,$AH,[System.Drawing.GraphicsUnit]::Pixel,$ia)
$gi.Dispose(); $img.Dispose()

# ---- canvas -------------------------------------------------------------------
$bmp = New-Object System.Drawing.Bitmap -ArgumentList $W,$H,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint=[System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$grect = New-Object System.Drawing.Rectangle -ArgumentList 0,0,$W,$H
$gb = New-Object System.Drawing.Drawing2D.LinearGradientBrush -ArgumentList $grect,$SAGE_TOP,$SAGE_BOT,([System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
$g.FillRectangle($gb,$grect); $gb.Dispose()

# ---- portrait medallion on the left ------------------------------------------
$CX = 540; $CY = 500; $R = 320
$brP = New-Object System.Drawing.SolidBrush -ArgumentList $PAPER
$g.FillEllipse($brP, ($CX-$R), ($CY-$R), (2*$R), (2*$R)); $brP.Dispose()
$gp = New-Object System.Drawing.Drawing2D.GraphicsPath
$gp.AddEllipse(($CX-$R), ($CY-$R), (2*$R), (2*$R))
$g.SetClip($gp)
$ph = 2*$R*0.80; $pw = $ph*$MW/$MH
$g.DrawImage($ink, [float]($CX-$pw/2), [float]($CY-$ph/2), [float]$pw, [float]$ph)
$g.ResetClip(); $gp.Dispose()

# ---- Hebrew type block, right-aligned ----------------------------------------
$RIGHT = 1800
$LEFTLIMIT = 920
$MAXW = $RIGHT - $LEFTLIMIT

function Fit-Font { param([string]$Text, [int]$Px, [System.Drawing.FontStyle]$Style, [int]$Max)
  $f = Pick-Font $Px $Style
  while ($f.Size -gt 12) {
    $w = $g.MeasureString($Text, $f).Width
    if ($w -le $Max) { break }
    $np = [int]($f.Size * 0.94); $f.Dispose(); $f = Pick-Font $np $Style
  }
  return $f
}
function Draw-Right { param([string]$Text, $Font, $Color, [int]$Y)
  $w = $g.MeasureString($Text, $Font).Width
  $br = New-Object System.Drawing.SolidBrush -ArgumentList $Color
  $g.DrawString($Text, $Font, $br, [float]($RIGHT - $w), [float]$Y)
  $br.Dispose()
  return $g.MeasureString($Text, $Font).Height
}

$TITLE    = 'אורות'
$SUBTITLE = 'הראי״ה קוק זצ״ל'
$TAGLINE  = 'חיפוש · סימניות · הערות · ללא אינטרנט'

$fT = Fit-Font $TITLE    190 ([System.Drawing.FontStyle]::Bold)    $MAXW
$fS = Fit-Font $SUBTITLE  72 ([System.Drawing.FontStyle]::Regular) $MAXW
$fG = Fit-Font $TAGLINE   54 ([System.Drawing.FontStyle]::Regular) $MAXW
Write-Output ("font sizes px -> title {0}  subtitle {1}  tagline {2}" -f $fT.Size, $fS.Size, $fG.Size)

$hT = $g.MeasureString($TITLE,$fT).Height
$hS = $g.MeasureString($SUBTITLE,$fS).Height
$hG = $g.MeasureString($TAGLINE,$fG).Height
$GAP1 = 4; $GAP2 = 46; $RULE = 3; $GAP3 = 44
$total = $hT + $GAP1 + $hS + $GAP2 + $RULE + $GAP3 + $hG
$y = ($H - $total)/2

$null = Draw-Right $TITLE    $fT $CREAM ([int]$y);              $y += $hT + $GAP1
$null = Draw-Right $SUBTITLE $fS $MUTED ([int]$y);              $y += $hS + $GAP2
$ruleW = $g.MeasureString($TAGLINE,$fG).Width
$pen = New-Object System.Drawing.Pen -ArgumentList ([System.Drawing.Color]::FromArgb(90,$CREAM.R,$CREAM.G,$CREAM.B)), ([float]$RULE)
$g.DrawLine($pen, [float]($RIGHT-$ruleW), [float]$y, [float]$RIGHT, [float]$y); $pen.Dispose()
$y += $RULE + $GAP3
$null = Draw-Right $TAGLINE  $fG $FAINT ([int]$y)

$fT.Dispose(); $fS.Dispose(); $fG.Dispose(); $g.Dispose()

# ---- downsample to the exact 1024x500 Play wants ------------------------------
$out = New-Object System.Drawing.Bitmap -ArgumentList ($W/$S),($H/$S),([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$go = [System.Drawing.Graphics]::FromImage($out)
$go.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$go.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$go.DrawImage($bmp, 0, 0, [int]($W/$S), [int]($H/$S))
$go.Dispose()
$path = 'docs/icon/feature-graphic-1024x500.png'
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ("wrote {0}  {1}x{2}" -f $path, $out.Width, $out.Height)
$out.Dispose(); $bmp.Dispose(); $ink.Dispose()
