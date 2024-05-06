using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;

namespace Bit.Core.Services
{
    public class AssetLinksService : IAssetLinksService
    {
        private readonly IApiService _apiService;

        public AssetLinksService(IApiService apiService)
        {
            _apiService = apiService;
        }

        /// <summary>
        /// Gets the digital asset links file associated with the <paramref name="rpId"/> and
        /// validates that the <paramref name="packageName"/> and <paramref name="normalizedFingerprint"/> matches.
        /// </summary>
        /// <returns><c>True</c> if matches, <c>False</c> otherwise.</returns>
        public async Task<bool> ValidateAssetLinksAsync(string rpId, string packageName, string normalizedFingerprint)
        {
            try
            {
                var statementList = await _apiService.GetDigitalAssetLinksForRpAsync(rpId);

                var androidAppPackageStatements = statementList
                    .Where(s => s.Target.Namespace == "android_app" 
                                && 
                                s.Target.PackageName == packageName
                                &&
                                s.Relation.Contains("delegate_permission/common.get_login_creds")
                                &&
                                s.Relation.Contains("delegate_permission/common.handle_all_urls"));

                if (!androidAppPackageStatements.Any())
                {
                    throw new Exceptions.ValidationException(AppResources.PasskeyOperationFailedBecauseAppNotFoundInAssetLinks);
                }

                if (!androidAppPackageStatements.Any(s => s.Target.Sha256CertFingerprints.Contains(normalizedFingerprint)))
                {
                    throw new Exceptions.ValidationException(AppResources.PasskeyOperationFailedBecauseAppCouldNotBeVerified);
                }

                return true;
            }
            catch (Exceptions.ApiException)
            {
                throw new Exceptions.ValidationException(AppResources.PasskeyOperationFailedBecauseOfMissingAssetLinks);
            }
        }
    }
}
