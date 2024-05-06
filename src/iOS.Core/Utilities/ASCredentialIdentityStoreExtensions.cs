using AuthenticationServices;
using Foundation;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class ASCredentialIdentityStoreExtensions
    {
        /// <summary>
        /// Saves password credential identities to the shared store of <see cref="ASCredentialIdentityStore"/>
        /// Note: This is added to provide the proper method depending on the OS version.
        /// </summary>
        /// <param name="identities">Password identities to save</param>
        public static Task<Tuple<bool, NSError>> SaveCredentialIdentitiesAsync(params ASPasswordCredentialIdentity[] identities)
        {
            if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return ASCredentialIdentityStore.SharedStore.SaveCredentialIdentityEntriesAsync(identities);
            }

            return ASCredentialIdentityStore.SharedStore.SaveCredentialIdentitiesAsync(identities);
        }

        /// <summary>
        /// Removes password credential identities of the shared store of <see cref="ASCredentialIdentityStore"/>
        /// Note: This is added to provide the proper method depending on the OS version.
        /// </summary>
        /// <param name="identities">Password identities to remove</param>
        public static Task<Tuple<bool, NSError>> RemoveCredentialIdentitiesAsync(params ASPasswordCredentialIdentity[] identities)
        {
            if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return ASCredentialIdentityStore.SharedStore.RemoveCredentialIdentityEntriesAsync(identities);
            }

            return ASCredentialIdentityStore.SharedStore.RemoveCredentialIdentitiesAsync(identities);
        }
    }
}
