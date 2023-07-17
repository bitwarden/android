using System;
using System.IO;
using System.Linq;
using System.Security.Cryptography.X509Certificates;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.Security;
using AndroidX.Activity.Result;
using AndroidX.Activity.Result.Contract;
using Bit.Core.Abstractions;
using Bit.Core.Models;
using Bit.Core.Utilities;
using Bit.Droid.Security;
using Bit.Droid.Utilities;
using Java.Interop;
using Java.Math;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Operators;
using Org.BouncyCastle.X509;
using Plugin.CurrentActivity;

namespace Bit.Droid.Services
{
    public class X509CertificateGenerator
    {
        public static System.Security.Cryptography.X509Certificates.X509Certificate2 GenerateX509Certificate()
        {
            try
            {
                // Generate a key pair (public and private key)
                RsaKeyPairGenerator keyPairGenerator = new RsaKeyPairGenerator();
                keyPairGenerator.Init(new KeyGenerationParameters(new Org.BouncyCastle.Security.SecureRandom(), 2048));
                AsymmetricCipherKeyPair keyPair = keyPairGenerator.GenerateKeyPair();

                // Set certificate information
                BigInteger serialNumber = BigInteger.ValueOf(DateTime.Now.Ticks);
                DateTime startDate = DateTime.Now; // Certificate valid from current date
                DateTime expiryDate = DateTime.Now.AddYears(1); // Certificate valid for 1 year

                System.Security.Cryptography.X509Certificates.X509Certificate2 cert = GenerateCertificate(keyPair, serialNumber, startDate, expiryDate, "CN=MyApp");

                return cert;
            }
            catch (Exception e)
            {
                System.Console.WriteLine(e.StackTrace);
                throw;
            }
        }

        public static (Org.BouncyCastle.X509.X509Certificate, AsymmetricCipherKeyPair) GenerateX509Certificate2()
        {
            try
            {
                // Generate a key pair (public and private key)
                RsaKeyPairGenerator keyPairGenerator = new RsaKeyPairGenerator();
                keyPairGenerator.Init(new KeyGenerationParameters(new Org.BouncyCastle.Security.SecureRandom(), 2048));
                AsymmetricCipherKeyPair keyPair = keyPairGenerator.GenerateKeyPair();

                // Set certificate information
                BigInteger serialNumber = BigInteger.ValueOf(DateTime.Now.Ticks);
                DateTime startDate = DateTime.Now; // Certificate valid from current date
                DateTime expiryDate = DateTime.Now.AddYears(1); // Certificate valid for 1 year

                Org.BouncyCastle.X509.X509Certificate cert = GenerateCertificate2(keyPair, serialNumber, startDate, expiryDate, "CN=MyApp");

                return (cert, keyPair);
            }
            catch (Exception e)
            {
                System.Console.WriteLine(e.StackTrace);
                throw;
            }
        }

        // Helper method to generate an X509 certificate
        private static System.Security.Cryptography.X509Certificates.X509Certificate2 GenerateCertificate(AsymmetricCipherKeyPair keyPair, BigInteger serialNumber, DateTime startDate, DateTime expiryDate, string subject)
        {
            try
            {
                X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
                certGenerator.SetSerialNumber(Org.BouncyCastle.Math.BigInteger.ValueOf(Java.Lang.JavaSystem.CurrentTimeMillis()));
                certGenerator.SetIssuerDN(new X509Name(subject));
                certGenerator.SetSubjectDN(new X509Name(subject));
                certGenerator.SetNotBefore(startDate);
                certGenerator.SetNotAfter(expiryDate);
                certGenerator.SetPublicKey(keyPair.Public);

                ISignatureFactory signatureFactory = new Asn1SignatureFactory("SHA256WITHRSA",
                                                                              keyPair.Private,
                                                                              new Org.BouncyCastle.Security.SecureRandom());

                Org.BouncyCastle.X509.X509Certificate certificate = certGenerator.Generate(signatureFactory);

                // Convert Bouncy Castle X509Certificate to the .NET X509Certificate2 format
                byte[] derEncodedCertificate = certificate.GetEncoded();
                X509Certificate2 x509Certificate2 = new X509Certificate2(derEncodedCertificate, "securepassword",
                    X509KeyStorageFlags.MachineKeySet | X509KeyStorageFlags.PersistKeySet | X509KeyStorageFlags.Exportable);

                // Return the .NET X509Certificate2 instance
                return x509Certificate2;
            }
            catch (Exception e)
            {
                System.Console.WriteLine(e.StackTrace);
                throw;
            }
        }

        private static Org.BouncyCastle.X509.X509Certificate GenerateCertificate2(AsymmetricCipherKeyPair keyPair, BigInteger serialNumber, DateTime startDate, DateTime expiryDate, string subject)
        {
            try
            {
                X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
                certGenerator.SetSerialNumber(Org.BouncyCastle.Math.BigInteger.ValueOf(Java.Lang.JavaSystem.CurrentTimeMillis()));
                certGenerator.SetIssuerDN(new X509Name(subject));
                certGenerator.SetSubjectDN(new X509Name(subject));
                certGenerator.SetNotBefore(startDate);
                certGenerator.SetNotAfter(expiryDate);
                certGenerator.SetPublicKey(keyPair.Public);

                ISignatureFactory signatureFactory = new Asn1SignatureFactory("SHA256WITHRSA",
                                                                              keyPair.Private,
                                                                              new Org.BouncyCastle.Security.SecureRandom());

                Org.BouncyCastle.X509.X509Certificate certificate = certGenerator.Generate(signatureFactory);

                return certificate;
            }
            catch (Exception e)
            {
                System.Console.WriteLine(e.StackTrace);
                throw;
            }
        }
    }

    public class CertificateService : ICertificateService
    {
        private readonly IFileService _fileService;
        private class ChoosePrivateKeyAliasCallback : Java.Lang.Object, IKeyChainAliasCallback
        {
            public Action<string> Success { get; set; }
            public Action Failed { get; set; }

            public void Alias(string alias)
            {
                if (string.IsNullOrEmpty(alias))
                {
                    this.Failed();
                    return;
                }

                this.Success.Invoke(alias);
            }
        }
        private class InstallKeyChainActivityResultCallback : Java.Lang.Object, IActivityResultCallback
        {
            private readonly Action<ActivityResult> _handler;
            public InstallKeyChainActivityResultCallback(Action<ActivityResult> handler)
            {
                _handler = handler;
            }

            public void OnActivityResult(Java.Lang.Object result)
            {
                ActivityResult activityResult = result as ActivityResult;
                _handler(activityResult);
            }
        }

        public CertificateService()
        {
            _fileService = ServiceContainer.Resolve<IFileService>();
        }

        public ICertificateSpec GetCertificate(string alias)
        {
            var certSpec = new X509CertificateSpec();

            var certChain = KeyChain.GetCertificateChain(Application.Context, alias).FirstOrDefault();

            var privateKeyRef = KeyChain.GetPrivateKey(Application.Context, alias);

            certSpec.Certificate = certChain;
            certSpec.Alias = alias;
            certSpec.PrivateKeyRef = privateKeyRef;

            return certSpec;
        }

        public Task<bool> InstallCertificateAsync()
        {
            var result = new TaskCompletionSource<bool>();

            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;

            try
            {
                _fileService.SelectFileAsync((ActivityResult activityResult) =>
                {
                    var data = activityResult?.Data;

                    if (activityResult == null ||
                        activityResult.ResultCode != (int)Result.Ok ||
                        data == null ||
                        data.Data == null)
                    {
                        result.SetResult(false);
                        return;
                    }

                    Android.Net.Uri uri = data.Data;

                    try
                    {
                        var intent = KeyChain.CreateInstallIntent();
                        using (var stream = activity.ContentResolver.OpenInputStream(uri))
                        using (var memoryStream = new MemoryStream())
                        {
                            stream.CopyTo(memoryStream);

                            intent.PutExtra(KeyChain.ExtraPkcs12, memoryStream.ToArray());
                            intent.PutExtra(KeyChain.ExtraName, "Bitwarden Client Certificate");
                        }

                        ActivityResultLauncher activityResultLauncher = activity.RegisterForActivityResult(
                            new ActivityResultContracts.StartActivityForResult(),
                            new InstallKeyChainActivityResultCallback((ActivityResult installKeyChainResult) =>
                                {
                                    result.SetResult(installKeyChainResult?.ResultCode == (int)Result.Ok);
                                })
                            );

                        activityResultLauncher.Launch(intent);
                    }
                    catch (Java.IO.FileNotFoundException)
                    {
                        result.SetResult(false);
                    }
                });
            }
            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine("No certificate selected", e.Message);

                result.SetResult(false);
            }

            return result.Task;
        }

        public Task<string> ChooseCertificateAsync(string alias = null)
        {
            var result = new TaskCompletionSource<string>();
            KeyChain.ChoosePrivateKeyAlias(
                CrossCurrentActivity.Current.Activity,
                new ChoosePrivateKeyAliasCallback
                {
                    Success = chosenAlias => result.SetResult(chosenAlias),
                    Failed = () => result.SetResult(string.Empty)
                },
                new string[] { "RSA" },
                null,
                null,
                -1,
                alias
            );
            return result.Task;
        }
    }
}
