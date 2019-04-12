using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Linq.Expressions;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class CipherService
    {
        private const string Keys_CiphersFormat = "ciphers_{0}";
        private const string Keys_LocalData = "ciphersLocalData";
        private const string Keys_NeverDomains = "neverDomains";

        private List<CipherView> _decryptedCipherCache;
        private readonly ICryptoService _cryptoService;
        private readonly IUserService _userService;
        private readonly ISettingsService _settingsService;
        private readonly IApiService _apiService;
        private readonly IStorageService _storageService;
        private readonly II18nService _i18NService;
        private Dictionary<string, HashSet<string>> _domainMatchBlacklist = new Dictionary<string, HashSet<string>>
        {
            ["google.com"] = new HashSet<string> { "script.google.com" }
        };

        public CipherService(
            ICryptoService cryptoService,
            IUserService userService,
            ISettingsService settingsService,
            IApiService apiService,
            IStorageService storageService,
            II18nService i18nService)
        {
            _cryptoService = cryptoService;
            _userService = userService;
            _settingsService = settingsService;
            _apiService = apiService;
            _storageService = storageService;
            _i18NService = i18nService;
        }

        private List<CipherView> DecryptedCipherCache
        {
            get => _decryptedCipherCache;
            set
            {
                if(value == null)
                {
                    _decryptedCipherCache.Clear();
                }
                _decryptedCipherCache = value;
                // TODO: update search index
            }
        }

        public void ClearCache()
        {
            DecryptedCipherCache = null;
        }

        public async Task<Cipher> Encrypt(CipherView model, SymmetricCryptoKey key = null,
            Cipher originalCipher = null)
        {
            // Adjust password history
            if(model.Id != null)
            {
                // TODO
            }

            var cipher = new Cipher();
            cipher.Id = model.Id;
            // TODO others

            if(key == null && cipher.OrganizationId != null)
            {
                key = await _cryptoService.GetOrgKeyAsync(cipher.OrganizationId);
                if(key == null)
                {
                    throw new Exception("Cannot encrypt cipher for organization. No key.");
                }
            }

            var tasks = new List<Task>();
            tasks.Add(EncryptObjPropertyAsync(model, cipher, new HashSet<string>
            {
                nameof(model.Name)
            }, key));

            await Task.WhenAll(tasks);
            return cipher;
        }

        private Task EncryptObjPropertyAsync<V, D>(V model, D obj, HashSet<string> map, SymmetricCryptoKey key)
            where V : View
            where D : Domain
        {
            var modelType = model.GetType();
            var objType = obj.GetType();

            Task<CipherString> makeCs(string propName)
            {
                var modelPropInfo = modelType.GetProperty(propName);
                var modelProp = modelPropInfo.GetValue(model) as string;
                if(!string.IsNullOrWhiteSpace(modelProp))
                {
                    return _cryptoService.EncryptAsync(modelProp, key);
                }
                return Task.FromResult((CipherString)null);
            };
            void setCs(string propName, CipherString val)
            {
                var objPropInfo = objType.GetProperty(propName);
                objPropInfo.SetValue(obj, val, null);
            };

            var tasks = new List<Task>();
            foreach(var prop in map)
            {
                tasks.Add(makeCs(prop).ContinueWith(async val => setCs(prop, await val)));
            }
            return Task.WhenAll(tasks);
        }
    }
}
