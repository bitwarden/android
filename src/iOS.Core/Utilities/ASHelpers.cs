using AuthenticationServices;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Foundation;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class ASHelpers
    {
        public static async Task ReplaceAllIdentitiesAsync()
        {
            if (!await IsAutofillEnabledAsync())
            {
                return;
            }

            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var stateService = ServiceContainer.Resolve<IStateService>();
            var timeoutAction = await stateService.GetVaultTimeoutActionAsync();
            if (timeoutAction == VaultTimeoutAction.Logout)
            {
                await ASCredentialIdentityStore.SharedStore.RemoveAllCredentialIdentitiesAsync();
                return;
            }

            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>();
            if (await vaultTimeoutService.IsLockedAsync())
            {
                await ASCredentialIdentityStore.SharedStore.RemoveAllCredentialIdentitiesAsync();
                await storageService.SaveAsync(Constants.AutofillNeedsIdentityReplacementKey, true);
                return;
            }

            var cipherService = ServiceContainer.Resolve<ICipherService>();
            if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                await ReplaceAllIdentitiesIOS17Async(cipherService, storageService);
            }
            else
            {
                await ReplaceAllIdentitiesIOS12Async(cipherService, storageService);
            }
        }

        private static async Task ReplaceAllIdentitiesIOS12Async(ICipherService cipherService, IStorageService storageService)
        {
            var ciphers = await cipherService.GetAllDecryptedAsync();
            var identities = ciphers.Where(c => !c.IsDeleted)
                                    .Select(ToPasswordCredentialIdentity)
                                    .Where(i => i != null)
                                    .Cast<ASPasswordCredentialIdentity>()
                                    .ToList();
            if (!identities.Any())
            {
                await ASCredentialIdentityStore.SharedStore.RemoveAllCredentialIdentitiesAsync();
                return;
            }

#pragma warning disable CA1422 // Validate platform compatibility
            await ASCredentialIdentityStore.SharedStore.ReplaceCredentialIdentitiesAsync(identities.ToArray());
#pragma warning restore CA1422 // Validate platform compatibility
            await storageService.SaveAsync(Constants.AutofillNeedsIdentityReplacementKey, false);
        }

        private static async Task ReplaceAllIdentitiesIOS17Async(ICipherService cipherService, IStorageService storageService)
        {
            var ciphers = await cipherService.GetAllDecryptedAsync();
            var identities = ciphers.Where(c => !c.IsDeleted)
                                    .Select(ToCredentialIdentity)
                                    .Where(i => i != null)
                                    .Cast<IASCredentialIdentity>()
                                    .ToList();
            if (!identities.Any())
            {
                await ASCredentialIdentityStore.SharedStore.RemoveAllCredentialIdentitiesAsync();
                return;
            }

            await ASCredentialIdentityStore.SharedStore.ReplaceCredentialIdentityEntriesAsync(identities.ToArray());
            await storageService.SaveAsync(Constants.AutofillNeedsIdentityReplacementKey, false);
        }

        public static async Task<bool> IdentitiesSupportIncrementalAsync()
        {
            var stateService = ServiceContainer.Resolve<IStateService>();
            var timeoutAction = await stateService.GetVaultTimeoutActionAsync();
            if (timeoutAction == VaultTimeoutAction.Logout)
            {
                return false;
            }
            var state = await ASCredentialIdentityStore.SharedStore.GetCredentialIdentityStoreStateAsync();
            return state != null && state.Enabled && state.SupportsIncrementalUpdates;
        }

        public static async Task<bool> IsAutofillEnabledAsync()
        {
            var state = await ASCredentialIdentityStore.SharedStore.GetCredentialIdentityStoreStateAsync();
            return state != null && state.Enabled;
        }

        public static async Task<ASPasswordCredentialIdentity?> GetCipherPasswordIdentityAsync(string cipherId)
        {
            var cipherService = ServiceContainer.Resolve<ICipherService>();
            var cipher = await cipherService.GetAsync(cipherId);
            if (cipher == null)
            {
                return null;
            }
            var cipherView = await cipher.DecryptAsync();
            return ToPasswordCredentialIdentity(cipherView);
        }

        public static ASPasswordCredentialIdentity? ToPasswordCredentialIdentity(CipherView cipher)
        {
            if (cipher?.Login?.Uris?.Any() != true)
            {
                return null;
            }
            var uri = cipher.Login.Uris.FirstOrDefault(u => u.Match != UriMatchType.Never)?.Uri;
            if (string.IsNullOrWhiteSpace(uri))
            {
                return null;
            }
            var username = cipher.Login.Username;
            if (string.IsNullOrWhiteSpace(username))
            {
                return null;
            }
            var serviceId = new ASCredentialServiceIdentifier(uri, ASCredentialServiceIdentifierType.Url);
            return new ASPasswordCredentialIdentity(serviceId, username, cipher.Id);
        }

        public static IASCredentialIdentity? ToCredentialIdentity(CipherView cipher)
        {
            try
            {
                if (!cipher.HasFido2Credential)
                {
                    return ToPasswordCredentialIdentity(cipher);
                }

                return new ASPasskeyCredentialIdentity(cipher.Login.MainFido2Credential.RpId,
                    cipher.GetMainFido2CredentialUsername(),
                    NSData.FromArray(cipher.Login.MainFido2Credential.CredentialId.GuidToRawFormat()),
                    cipher.Login.MainFido2Credential.UserHandle,
                    cipher.Id);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return null;
            }
        }
    }
}
