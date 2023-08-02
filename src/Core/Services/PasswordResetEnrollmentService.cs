using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class PasswordResetEnrollmentService : IPasswordResetEnrollmentService
    {
        private readonly IApiService _apiService;
        private readonly ICryptoService _cryptoService;
        private readonly IOrganizationService _organizationService;
        private readonly IStateService _stateService;

        public PasswordResetEnrollmentService(IApiService apiService,
            ICryptoService cryptoService,
            IOrganizationService organizationService,
            IStateService stateService)
        {
            _apiService = apiService;
            _cryptoService = cryptoService;
            _organizationService = organizationService;
            _stateService = stateService;
        }

        public async Task EnrollIfRequired(string organizationSsoId)
        {
            var orgAutoEnrollStatusResponse = await _apiService.GetOrganizationAutoEnrollStatusAsync(organizationSsoId);

            if (!orgAutoEnrollStatusResponse.ResetPasswordEnabled)
            {
                await this.Enroll(orgAutoEnrollStatusResponse.Id);
            }
        }

        public async Task Enroll(string organizationId)
        {
            var orgKeyResponse = await _apiService.GetOrganizationKeysAsync(organizationId);
            if (orgKeyResponse == null)
            {
                throw new Exception("Organization keys missing");
            }

            var userId = await _stateService.GetActiveUserIdAsync();
            var userKey = await _cryptoService.GetUserKeyAsync();
            var publicKey = CoreHelpers.Base64UrlDecode(orgKeyResponse.PublicKey);
            // RSA Encrypt user's userKey.key with organization public key
            var encryptedKey = await _cryptoService.RsaEncryptAsync(userKey.Key, publicKey);

            var resetRequest = new OrganizationUserResetPasswordEnrollmentRequest();
            resetRequest.ResetPasswordKey = encryptedKey.EncryptedString;

            await _apiService.PutOrganizationUserResetPasswordEnrollmentAsync(
              organizationId,
              userId,
              resetRequest
            );
        }
    }
}

