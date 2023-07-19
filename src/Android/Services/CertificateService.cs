using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Android.App;
using Android.Security;
using AndroidX.Activity.Result;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models;
using Bit.Core.Utilities;
using Bit.Droid.Security;
using Java.Security;
using Java.Security.Cert;
using Plugin.CurrentActivity;
using Xamarin.Forms;
using Task = System.Threading.Tasks.Task;

namespace Bit.Droid.Services
{
    public class CertificateService : ICertificateService
    {
        private const string DefaultClientCertAlias = "ClientCertificate";

        private const string HostKeyChain = "keychain";
        private const string HostKeyStore = "keystore";

        private readonly IFileService _fileService;
        private readonly IDeviceActionService _deviceActionService;

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

        public CertificateService()
        {
            _fileService = ServiceContainer.Resolve<IFileService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
        }

        public bool TryRemoveCertificate(string certUri)
        {
            try
            {
                var uri = new Uri(certUri);

                var keyHost = uri.Authority;
                var alias = uri.LocalPath.TrimEnd('/');

                if (keyHost == HostKeyStore)
                {
                    KeyStore keyStore = KeyStore.GetInstance("AndroidKeyStore");
                    keyStore.DeleteEntry(alias);
                }
                else if (keyHost == HostKeyChain)
                {
                    // skip. nothing to do
                }

                return true;
            }
            catch
            {
                return false;
            }
        }

        public async Task<ICertificateChainSpec> GetCertificateAsync(string certUri)
        {
            var certSpec = new X509CertificateChainSpec();
            if (string.IsNullOrEmpty(certUri)) return certSpec;

            var uri = new Uri(certUri);

            var keyHost = uri.Host;
            var alias = uri.LocalPath.Trim('/');


            if (keyHost == HostKeyStore)
            {
                KeyStore keyStore = KeyStore.GetInstance("AndroidKeyStore");
                keyStore.Load(null);
                
                var certChain = keyStore.GetCertificateChain(alias);
                var privateKeyRef = keyStore.GetKey(alias, null);

                certSpec.CertificateChain = certChain.Cast<X509Certificate>().ToArray();
                certSpec.Alias = alias;
                certSpec.PrivateKeyRef = privateKeyRef;

                keyStore.Dispose();
            }
            else if (keyHost == HostKeyChain)
            {
                await Task.Run(() =>
                {
                    certSpec.CertificateChain = KeyChain.GetCertificateChain(Android.App.Application.Context, alias);
                    certSpec.PrivateKeyRef = KeyChain.GetPrivateKey(Android.App.Application.Context, alias);
                });

                certSpec.Alias = alias;
            }

            return certSpec;
        }

        public async Task<string> ImportCertificateAsync()
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                ActivityResult activityResult = await _fileService.SelectFileAsync<ActivityResult>(); ;

                var data = activityResult?.Data;

                if (activityResult == null ||
                    activityResult.ResultCode != (int)Result.Ok ||
                    data == null ||
                    data.Data == null)
                {
                    return string.Empty;
                }

                string chosenCertAlias = string.Empty;

                var password = await _deviceActionService.DisplayPromptAync(AppResources.Password, AppResources.PasswordEnterDecryptCert, 
                    null, AppResources.Submit, AppResources.Cancel, password: true);
                
                if (password == null) 
                {
                    return string.Empty;
                }

                using (var stream = activity.ContentResolver.OpenInputStream(data.Data))
                {
                    // Step 1: Load PKCS#12 bytes into a KeyStore
                    KeyStore pkcs12KeyStore = KeyStore.GetInstance("pkcs12");
                    pkcs12KeyStore.Load(stream, password.ToCharArray());

                    // Step 2: Get list of aliases and choose first one
                    List<string> aliases = new List<string>();

                    var aliasEnumeration = pkcs12KeyStore.Aliases();
                    if (!aliasEnumeration.HasMoreElements)
                    {
                        return string.Empty;
                    }
                    chosenCertAlias = aliasEnumeration.NextElement().ToString();

                    // Step 3: Extract PrivateKey and X.509 certificate from the KeyStore and verify cert. alias
                    IKey privateKey = pkcs12KeyStore.GetKey(chosenCertAlias, password.ToCharArray());
                    Certificate[] certificateChain = pkcs12KeyStore.GetCertificateChain(chosenCertAlias);

                    pkcs12KeyStore.Dispose();

                    if (privateKey == null || certificateChain == null || certificateChain.Length == 0)
                    {
                        // Handle error: unable to extract the private key or certificate
                        return string.Empty;
                    }

                    if (string.IsNullOrEmpty(chosenCertAlias))
                    {
                        chosenCertAlias = DefaultClientCertAlias;
                    }

                    var certUri = VerifyAndFormatCertUri(chosenCertAlias);

                    // Step 4: Create an Android KeyStore instance
                    KeyStore androidKeyStore = KeyStore.GetInstance("AndroidKeyStore");
                    androidKeyStore.Load(null);

                    // Step 5: Store the private key and X.509 certificate in the Android KeyStore
                    androidKeyStore.SetKeyEntry(chosenCertAlias, privateKey, null, certificateChain);

                    androidKeyStore.Dispose();
                    return certUri;
                }
            }
            catch (Exception e)
            {
                _deviceActionService.Toast($"Failed to import the certificate\n {e.Message}");
                return string.Empty;
            }
        }

        public Task<string> ChooseSystemCertificateAsync()
        {
            var result = new TaskCompletionSource<string>();
            try
            {
                Device.BeginInvokeOnMainThread(() =>
                {
                    try
                    {
                        KeyChain.ChoosePrivateKeyAlias(
                            CrossCurrentActivity.Current.Activity,
                            new ChoosePrivateKeyAliasCallback
                            {
                                Success = chosenAlias => result.SetResult(VerifyAndFormatCertUri(chosenAlias, true)),
                                Failed = () => result.SetResult(string.Empty),
                            },
                            new string[] { "RSA" },
                            null,
                            null,
                            -1,
                            null);
                    }
                    catch (Exception ex)
                    {
                        result.SetException(ex);
                    }

                });
            }
            catch (Exception ex)
            {
                result.SetException(ex);
            }

            return result.Task;
        }

        private string VerifyAndFormatCertUri(string alias, bool locatedInSystemVault = false)
        {
            if (string.IsNullOrEmpty(alias) || alias.StartsWith("cert://") || alias.Contains("/"))
            {
                throw new Exception("Invalid certificate alias!");
            }

            if (locatedInSystemVault)
            {
                return $"cert://{HostKeyChain}/{alias}";
            }
            else
            {
                return $"cert://{HostKeyStore}/{alias}";
            }
        }
    }
}
