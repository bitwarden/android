using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Linq;
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
                if(originalCipher == null)
                {
                    originalCipher = await GetAsync(model.Id);
                }
                if(originalCipher != null)
                {
                    var existingCipher = await originalCipher.DecryptAsync();
                    if(model.PasswordHistory == null)
                    {
                        model.PasswordHistory = new List<PasswordHistoryView>();
                    }
                    if(model.Type == CipherType.Login && existingCipher.Type == CipherType.Login)
                    {
                        if(!string.IsNullOrWhiteSpace(existingCipher.Login.Password) &&
                            existingCipher.Login.Password != model.Login.Password)
                        {
                            var now = DateTime.UtcNow;
                            var ph = new PasswordHistoryView
                            {
                                Password = existingCipher.Login.Password,
                                LastUsedDate = now
                            };
                            model.Login.PasswordRevisionDate = now;
                            model.PasswordHistory.Insert(0, ph);
                        }
                        else
                        {
                            model.Login.PasswordRevisionDate = DateTime.UtcNow;
                        }
                    }
                    if(existingCipher.HasFields)
                    {
                        var existingHiddenFields = existingCipher.Fields.Where(f =>
                            f.Type == FieldType.Hidden && !string.IsNullOrWhiteSpace(f.Name) &&
                            !string.IsNullOrWhiteSpace(f.Value));
                        var hiddenFields = model.Fields?.Where(f =>
                            f.Type == FieldType.Hidden && !string.IsNullOrWhiteSpace(f.Name)) ??
                            new List<FieldView>();
                        foreach(var ef in existingHiddenFields)
                        {
                            var matchedField = hiddenFields.FirstOrDefault(f => f.Name == ef.Name);
                            if(matchedField == null || matchedField.Value != ef.Value)
                            {
                                var ph = new PasswordHistoryView
                                {
                                    Password = string.Format("{0}: {1}", ef.Name, ef.Value),
                                    LastUsedDate = DateTime.UtcNow
                                };
                                model.PasswordHistory.Insert(0, ph);
                            }
                        }
                    }
                }
                if(!model.PasswordHistory?.Any() ?? false)
                {
                    model.PasswordHistory = null;
                }
                else if(model.PasswordHistory != null && model.PasswordHistory.Count > 5)
                {
                    model.PasswordHistory = model.PasswordHistory.Take(5).ToList();
                }
            }

            var cipher = new Cipher
            {
                Id = model.Id,
                FolderId = model.FolderId,
                Favorite = model.Favorite,
                OrganizationId = model.OrganizationId,
                Type = model.Type,
                CollectionIds = model.CollectionIds
            };

            if(key == null && cipher.OrganizationId != null)
            {
                key = await _cryptoService.GetOrgKeyAsync(cipher.OrganizationId);
                if(key == null)
                {
                    throw new Exception("Cannot encrypt cipher for organization. No key.");
                }
            }

            var tasks = new List<Task>
            {
                EncryptObjPropertyAsync(model, cipher, new HashSet<string>
                {
                   "Name",
                   "Notes"
                }, key),
                EncryptCipherDataAsync(cipher, model, key),
                EncryptFieldsAsync(model.Fields, key)
                    .ContinueWith(async fields => cipher.Fields = await fields),
                EncryptPasswordHistoriesAsync(model.PasswordHistory, key)
                    .ContinueWith(async phs => cipher.PasswordHistory = await phs),
                EncryptAttachmentsAsync(model.Attachments, key)
                    .ContinueWith(async attachments => cipher.Attachments = await attachments)
            };
            await Task.WhenAll(tasks);
            return cipher;
        }

        public async Task<Cipher> GetAsync(string id)
        {
            var userId = await _userService.GetUserIdAsync();
            var localData = await _storageService.GetAsync<Dictionary<string, Dictionary<string, object>>>(
                Keys_LocalData);
            var ciphers = await _storageService.GetAsync<Dictionary<string, CipherData>>(
                string.Format(Keys_CiphersFormat, userId));
            if(!ciphers?.ContainsKey(id) ?? true)
            {
                return null;
            }
            return new Cipher(ciphers[id], false,
                localData?.ContainsKey(id) ?? false ? localData[id] : null);
        }

        public async Task<List<Cipher>> GetAllAsync()
        {
            var userId = await _userService.GetUserIdAsync();
            var localData = await _storageService.GetAsync<Dictionary<string, Dictionary<string, object>>>(
                Keys_LocalData);
            var ciphers = await _storageService.GetAsync<Dictionary<string, CipherData>>(
                string.Format(Keys_CiphersFormat, userId));
            var response = ciphers.Select(c => new Cipher(c.Value, false,
                localData?.ContainsKey(c.Key) ?? false ? localData[c.Key] : null));
            return response.ToList();
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

        private async Task<List<Attachment>> EncryptAttachmentsAsync(
            List<AttachmentView> attachmentsModel, SymmetricCryptoKey key)
        {
            if(!attachmentsModel?.Any() ?? true)
            {
                return null;
            }
            var tasks = new List<Task>();
            var encAttachments = new List<Attachment>();
            foreach(var model in attachmentsModel)
            {
                var attachment = new Attachment
                {
                    Id = model.Id,
                    Size = model.Size,
                    SizeName = model.SizeName,
                    Url = model.Url
                };
                var task = EncryptObjPropertyAsync(model, attachment, new HashSet<string>
                {
                    "FileName"
                }, key).ContinueWith(async (t) =>
                {
                    if(model.Key != null)
                    {
                        attachment.Key = await _cryptoService.EncryptAsync(model.Key.Key, key);
                    }
                    encAttachments.Add(attachment);
                });
                tasks.Add(task);
            }
            await Task.WhenAll(tasks);
            return encAttachments;
        }

        private async Task EncryptCipherDataAsync(Cipher cipher, CipherView model, SymmetricCryptoKey key)
        {
            switch(cipher.Type)
            {
                case Enums.CipherType.Login:
                    cipher.Login = new Login
                    {
                        PasswordRevisionDate = model.Login.PasswordRevisionDate
                    };
                    await EncryptObjPropertyAsync(model.Login, cipher.Login, new HashSet<string>
                    {
                        "Username",
                        "Password",
                        "Totp"
                    }, key);
                    if(model.Login.Uris != null)
                    {
                        cipher.Login.Uris = new List<LoginUri>();
                        foreach(var uri in model.Login.Uris)
                        {
                            var loginUri = new LoginUri
                            {
                                Match = uri.Match
                            };
                            await EncryptObjPropertyAsync(uri, loginUri, new HashSet<string> { "Uri" }, key);
                            cipher.Login.Uris.Add(loginUri);
                        }
                    }
                    break;
                case Enums.CipherType.SecureNote:
                    cipher.SecureNote = new SecureNote
                    {
                        Type = model.SecureNote.Type
                    };
                    break;
                case Enums.CipherType.Card:
                    cipher.Card = new Card();
                    await EncryptObjPropertyAsync(model.Card, cipher.Card, new HashSet<string>
                    {
                        "CardholderName",
                        "Brand",
                        "Number",
                        "ExpMonth",
                        "ExpYear",
                        "Code"
                    }, key);
                    break;
                case Enums.CipherType.Identity:
                    cipher.Identity = new Identity();
                    await EncryptObjPropertyAsync(model.Identity, cipher.Identity, new HashSet<string>
                    {
                        "Title",
                        "FirstName",
                        "MiddleName",
                        "LastName",
                        "Address1",
                        "Address2",
                        "Address3",
                        "City",
                        "State",
                        "PostalCode",
                        "Country",
                        "Company",
                        "Email",
                        "Phone",
                        "SSN",
                        "Username",
                        "PassportNumber",
                        "LicenseNumber"
                    }, key);
                    break;
                default:
                    throw new Exception("Unknown cipher type.");
            }
        }

        private async Task<List<Field>> EncryptFieldsAsync(List<FieldView> fieldsModel, SymmetricCryptoKey key)
        {
            if(!fieldsModel?.Any() ?? true)
            {
                return null;
            }
            var tasks = new List<Task>();
            var encFields = new List<Field>();
            foreach(var model in fieldsModel)
            {
                var field = new Field
                {
                    Type = model.Type
                };
                // normalize boolean type field values
                if(model.Type == FieldType.Boolean && model.Value != "true")
                {
                    model.Value = "false";
                }
                var task = EncryptObjPropertyAsync(model, field, new HashSet<string>
                {
                    "Name",
                    "Value"
                }, key).ContinueWith((t) =>
                {
                    encFields.Add(field);
                });
                tasks.Add(task);
            }
            await Task.WhenAll(tasks);
            return encFields;
        }

        private async Task<List<PasswordHistory>> EncryptPasswordHistoriesAsync(List<PasswordHistoryView> phModels,
            SymmetricCryptoKey key)
        {
            if(!phModels?.Any() ?? true)
            {
                return null;
            }
            var tasks = new List<Task>();
            var encPhs = new List<PasswordHistory>();
            foreach(var model in phModels)
            {
                var ph = new PasswordHistory
                {
                    LastUsedDate = model.LastUsedDate
                };
                var task = EncryptObjPropertyAsync(model, ph, new HashSet<string>
                {
                    "Password"
                }, key).ContinueWith((t) =>
                {
                    encPhs.Add(ph);
                });
                tasks.Add(task);
            }
            await Task.WhenAll(tasks);
            return encPhs;
        }
    }
}
