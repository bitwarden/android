using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Models;

namespace Bit.iOS.Core.Utilities
{
    public static class ASHelpers
    {
        public static async Task ReplaceAllIdentities(ICipherService cipherService)
        {
            if (await AutofillEnabled())
            {
                var identities = new List<ASPasswordCredentialIdentity>();
                var ciphers = await cipherService.GetAllAsync();
                foreach (var cipher in ciphers)
                {
                    var identity = ToCredentialIdentity(cipher);
                    if (identity != null)
                    {
                        identities.Add(identity);
                    }
                }
                if (identities.Any())
                {
                    await ASCredentialIdentityStore.SharedStore.ReplaceCredentialIdentitiesAsync(identities.ToArray());
                }
            }
        }

        public static async Task<bool> IdentitiesCanIncremental()
        {
            var state = await ASCredentialIdentityStore.SharedStore.GetCredentialIdentityStoreStateAsync();
            return state.Enabled && state.SupportsIncrementalUpdates;
        }

        public static async Task<bool> AutofillEnabled()
        {
            var state = await ASCredentialIdentityStore.SharedStore.GetCredentialIdentityStoreStateAsync();
            return state.Enabled;
        }

        public static async Task<ASPasswordCredentialIdentity> GetCipherIdentityAsync(string cipherId, ICipherService cipherService)
        {
            var cipher = await cipherService.GetByIdAsync(cipherId);
            return ToCredentialIdentity(cipher);
        }

        public static ASPasswordCredentialIdentity ToCredentialIdentity(Cipher cipher)
        {
            if (!cipher?.Login?.Uris?.Any() ?? true)
            {
                return null;
            }
            var uri = cipher.Login.Uris.FirstOrDefault()?.Uri?.Decrypt(cipher.OrganizationId);
            if (string.IsNullOrWhiteSpace(uri))
            {
                return null;
            }
            var username = cipher.Login.Username?.Decrypt(cipher.OrganizationId);
            if (string.IsNullOrWhiteSpace(username))
            {
                return null;
            }
            var serviceId = new ASCredentialServiceIdentifier(uri, ASCredentialServiceIdentifierType.Url);
            return new ASPasswordCredentialIdentity(serviceId, username, cipher.Id);
        }
    }
}
