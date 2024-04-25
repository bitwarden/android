using Bit.Core.Abstractions;

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
            var statementList = await _apiService.GetDigitalAssetLinksForRpAsync(rpId);

            return statementList
                .Any(s => s.Target.Namespace == "android_app" 
                            && 
                            s.Target.PackageName == packageName
                            &&
                            s.Relation.Contains("delegate_permission/common.get_login_creds")
                            &&
                            s.Relation.Contains("delegate_permission/common.handle_all_urls")
                            &&
                            s.Target.Sha256CertFingerprints.Contains(normalizedFingerprint));
        }
    }
}
