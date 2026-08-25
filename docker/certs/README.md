# Extra root CAs for the local build image

Only needed on a network that inspects TLS - a corporate proxy such as
Netskope, Zscaler or Palo Alto. On such a network `docker compose ... verify`
fails while downloading the Android SDK:

```
curl: (60) SSL certificate OpenSSL verify result:
      self-signed certificate in certificate chain (19)
```

Your host trusts the proxy's root CA; a fresh container does not. Put that root
CA here as a PEM-encoded `.crt` and `Dockerfile.android` will install it into
both the OS trust store and the JDK's `cacerts`, so curl and Gradle both work.

Never work around this with `curl -k` or by disabling certificate checks - that
turns a working chain of trust into no trust at all.

## Exporting the CA on Windows

```powershell
$dir = "docker\certs"
$c = New-Object System.Net.Sockets.TcpClient('dl.google.com', 443)
$ssl = New-Object System.Net.Security.SslStream($c.GetStream(), $false, {$true})
$ssl.AuthenticateAsClient('dl.google.com')
$leaf = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate)
$chain = New-Object System.Security.Cryptography.X509Certificates.X509Chain
$null = $chain.Build($leaf)
$root = $chain.ChainElements[$chain.ChainElements.Count - 1].Certificate
$b64 = [Convert]::ToBase64String($root.RawData, 'InsertLineBreaks')
[IO.File]::WriteAllText("$dir\proxy-root.crt",
  "-----BEGIN CERTIFICATE-----`n$b64`n-----END CERTIFICATE-----`n" -replace "`r`n", "`n")
$ssl.Dispose(); $c.Dispose()
```

If the chain ends at a normal public CA, your network is not intercepting TLS
and you do not need any of this.

## Why the .crt files are gitignored

They identify your employer's network. They are not secret - a root CA is
public by design - but they are specific to one network and useless to anyone
else, so `.gitignore` keeps them out of the repo. Remove the `docker/certs/*.crt`
line from `.gitignore` if your team wants to share one.
