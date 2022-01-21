using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.iOS.Core.Utilities
{
    public static class ASHelpers
    {
        public static async Task ReplaceAllIdentities()
        {
            if (await AutofillEnabled())
            {
                var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
                var stateService = ServiceContainer.Resolve<IStateService>("stateService");
                var timeoutAction = await stateService.GetVaultTimeoutActionAsync();
                if (timeoutAction == "logOut")
                {
                    return;
                }
                var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
                if (await vaultTimeoutService.IsLockedAsync())
                {
                    await storageService.SaveAsync(Constants.AutofillNeedsIdentityReplacementKey, true);
                    return;
                }
                var cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
                var identities = new List<ASPasswordCredentialIdentity>();
                var ciphers = await cipherService.GetAllDecryptedAsync();
                foreach (var cipher in ciphers.Where(x => !x.IsDeleted))
                {
                    var identity = ToCredentialIdentity(cipher);
                    if (identity != null)
                    {
                        identities.Add(identity);
                    }
                }
                if (identities.Any())
                {
                    await ASCredentialIdentityStore.SharedStore?.ReplaceCredentialIdentitiesAsync(identities.ToArray());
                    await storageService.SaveAsync(Constants.AutofillNeedsIdentityReplacementKey, false);
                }
            }
        }

        public static async Task<bool> IdentitiesCanIncremental()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            var timeoutAction = await stateService.GetVaultTimeoutActionAsync();
            if (timeoutAction == "logOut")
            {
                return false;
            }
            var state = await ASCredentialIdentityStore.SharedStore?.GetCredentialIdentityStoreStateAsync();
            return state != null && state.Enabled && state.SupportsIncrementalUpdates;
        }

        public static async Task<bool> AutofillEnabled()
        {
            var state = await ASCredentialIdentityStore.SharedStore?.GetCredentialIdentityStoreStateAsync();
            return state != null && state.Enabled;
        }

        public static async Task<ASPasswordCredentialIdentity> GetCipherIdentityAsync(string cipherId)
        {
            var cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            var cipher = await cipherService.GetAsync(cipherId);
            if (cipher == null)
            {
                return null;
            }
            var cipherView = await cipher.DecryptAsync();
            return ToCredentialIdentity(cipherView);
        }

        public static ASPasswordCredentialIdentity ToCredentialIdentity(CipherView cipher)
        {
            if (!cipher?.Login?.Uris?.Any() ?? true)
            {
                return null;
            }
            var uri = cipher.Login.Uris.FirstOrDefault(u => u.Match != Bit.Core.Enums.UriMatchType.Never)?.Uri;
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
    }
}
