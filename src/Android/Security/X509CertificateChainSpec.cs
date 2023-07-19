using System;
using System.Text;
using Bit.Core.Models;
using Java.Security;
using Java.Security.Cert;

namespace Bit.Droid.Security
{
    public class X509CertificateChainSpec : ICertificateChainSpec<Java.Security.Cert.X509Certificate, IKey>
    {
        public string Alias { get; set; }

        public IKey PrivateKeyRef { get; internal set; }

        public X509Certificate RootCertificate
        {
            get => CertificateChain?[0];
        }
        public X509Certificate[] CertificateChain { get; set; }

        public X509Certificate LeafCertificate { 
            get => CertificateChain?[CertificateChain.Length-1];
        }

        public string ToString(string format, IFormatProvider formatProvider)
        {
            if (LeafCertificate == null) { 
                return string.Empty; 
            }

            StringBuilder sb = new StringBuilder();
            sb.AppendLine($"Subject: {LeafCertificate.SubjectDN}");
            sb.AppendLine($"Issuer: {LeafCertificate.IssuerDN}");
            sb.AppendLine($"Valid From: {LeafCertificate.NotBefore}");
            sb.AppendLine($"Valid Until: {LeafCertificate.NotAfter}");
            
            return sb.ToString();
        }

        public override string ToString() => this.ToString(null, System.Globalization.CultureInfo.CurrentCulture);
    }
}
