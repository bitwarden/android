using System;
using System.Text;
using Bit.Core.Models;
using Java.Security;

namespace Bit.Droid.Security
{
    public class X509CertificateSpec : ICertificateSpec<Java.Security.Cert.X509Certificate, IPrivateKey>
    {
        public Java.Security.Cert.X509Certificate Certificate { get; set; }

        public string Alias { get; set; }

        public IPrivateKey PrivateKeyRef { get; internal set; }

        public string ToString(string format, IFormatProvider formatProvider)
        {
            if (Certificate == null) { 
                return string.Empty; 
            }

            StringBuilder sb = new StringBuilder();
            sb.AppendLine($"Subject: {Certificate.SubjectDN}");
            sb.AppendLine($"Issuer: {Certificate.IssuerDN}");
            sb.AppendLine($"Valid From: {Certificate.NotBefore}");
            sb.AppendLine($"Valid Until: {Certificate.NotAfter}");
            
            return sb.ToString();
        }

        public override string ToString() => this.ToString(null, System.Globalization.CultureInfo.CurrentCulture);
    }
}
