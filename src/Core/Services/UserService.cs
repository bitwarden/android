using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class UserService : IUserService
    {
        private string _userId;
        private string _email;
        private string _stamp;
        private KdfType? _kdf;
        private int? _kdfIterations;
        private bool? _emailVerified;

        private const string Keys_UserId = "userId";
        private const string Keys_UserEmail = "userEmail";
        private const string Keys_Stamp = "securityStamp";
        private const string Keys_Kdf = "kdf";
        private const string Keys_KdfIterations = "kdfIterations";
        private const string Keys_OrganizationsFormat = "organizations_{0}";
        private const string Keys_EmailVerified = "emailVerified";

        private readonly IStorageService _storageService;
        private readonly ITokenService _tokenService;

        public UserService(IStorageService storageService, ITokenService tokenService)
        {
            _storageService = storageService;
            _tokenService = tokenService;
        }

        public async Task SetInformationAsync(string userId, string email, KdfType kdf, int? kdfIterations)
        {
            _email = email;
            _userId = userId;
            _kdf = kdf;
            _kdfIterations = kdfIterations;
            await Task.WhenAll(
                _storageService.SaveAsync(Keys_UserEmail, email),
                _storageService.SaveAsync(Keys_UserId, userId),
                _storageService.SaveAsync(Keys_Kdf, (int)kdf),
                _storageService.SaveAsync(Keys_KdfIterations, kdfIterations));
        }

        public async Task SetSecurityStampAsync(string stamp)
        {
            _stamp = stamp;
            await _storageService.SaveAsync(Keys_Stamp, stamp);
        }

        public async Task SetEmailVerifiedAsync(bool emailVerified)
        {
            _emailVerified = emailVerified;
            await _storageService.SaveAsync(Keys_EmailVerified, emailVerified);
        }

        public async Task<string> GetUserIdAsync()
        {
            if (_userId == null)
            {
                _userId = await _storageService.GetAsync<string>(Keys_UserId);
            }
            return _userId;
        }

        public async Task<string> GetEmailAsync()
        {
            if (_email == null)
            {
                _email = await _storageService.GetAsync<string>(Keys_UserEmail);
            }
            return _email;
        }

        public async Task<string> GetSecurityStampAsync()
        {
            if (_stamp == null)
            {
                _stamp = await _storageService.GetAsync<string>(Keys_Stamp);
            }
            return _stamp;
        }

        public async Task<bool> GetEmailVerifiedAsync()
        {
            if (_emailVerified == null)
            {
                _emailVerified = await _storageService.GetAsync<bool>(Keys_EmailVerified);
            }
            return _emailVerified.GetValueOrDefault();
        }

        public async Task<KdfType?> GetKdfAsync()
        {
            if (_kdf == null)
            {
                _kdf = (KdfType?)(await _storageService.GetAsync<int?>(Keys_Kdf));
            }
            return _kdf;
        }

        public async Task<int?> GetKdfIterationsAsync()
        {
            if (_kdfIterations == null)
            {
                _kdfIterations = await _storageService.GetAsync<int?>(Keys_KdfIterations);
            }
            return _kdfIterations;
        }

        public async Task ClearAsync()
        {
            var userId = await GetUserIdAsync();
            await Task.WhenAll(
                _storageService.RemoveAsync(Keys_UserId),
                _storageService.RemoveAsync(Keys_UserEmail),
                _storageService.RemoveAsync(Keys_Stamp),
                _storageService.RemoveAsync(Keys_Kdf),
                _storageService.RemoveAsync(Keys_KdfIterations),
                ClearOrganizationsAsync(userId));
            _userId = _email = _stamp = null;
            _kdf = null;
            _kdfIterations = null;
        }

        public async Task<bool> IsAuthenticatedAsync()
        {
            var token = await _tokenService.GetTokenAsync();
            if (token == null)
            {
                return false;
            }
            var userId = await GetUserIdAsync();
            return userId != null;
        }

        public async Task<bool> CanAccessPremiumAsync()
        {
            var authed = await IsAuthenticatedAsync();
            if (!authed)
            {
                return false;
            }

            var tokenPremium = _tokenService.GetPremium();
            if (tokenPremium)
            {
                return true;
            }
            var orgs = await GetAllOrganizationAsync();
            return orgs?.Any(o => o.UsersGetPremium && o.Enabled) ?? false;
        }

        public async Task<Organization> GetOrganizationAsync(string id)
        {
            var userId = await GetUserIdAsync();
            var organizations = await _storageService.GetAsync<Dictionary<string, OrganizationData>>(
                string.Format(Keys_OrganizationsFormat, userId));
            if (organizations == null || !organizations.ContainsKey(id))
            {
                return null;
            }
            return new Organization(organizations[id]);
        }

        public async Task<List<Organization>> GetAllOrganizationAsync()
        {
            var userId = await GetUserIdAsync();
            var organizations = await _storageService.GetAsync<Dictionary<string, OrganizationData>>(
                string.Format(Keys_OrganizationsFormat, userId));
            return organizations?.Select(o => new Organization(o.Value)).ToList() ?? new List<Organization>();
        }

        public async Task ReplaceOrganizationsAsync(Dictionary<string, OrganizationData> organizations)
        {
            var userId = await GetUserIdAsync();
            await _storageService.SaveAsync(string.Format(Keys_OrganizationsFormat, userId), organizations);
        }

        public async Task ClearOrganizationsAsync(string userId)
        {
            await _storageService.RemoveAsync(string.Format(Keys_OrganizationsFormat, userId));
        }
    }
}
