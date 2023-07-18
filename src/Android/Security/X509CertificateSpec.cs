using System;
using System.Text;
using Bit.Core.Models;
using Java.Security;
using Java.Security.Cert;

namespace Bit.Droid.Security
{
    public class X509CertificateSpec : ICertificateSpec<Java.Security.Cert.X509Certificate, IKey>
    {
        public X509Certificate Certificate { 
            get => CertificateChain?[0];
        }

        public string Alias { get; set; }

        public IKey PrivateKeyRef { get; internal set; }

        public X509Certificate[] CertificateChain { get; set; }

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
