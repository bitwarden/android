using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using System.Collections.Generic;
using Bit.Core.Models.Response;

namespace Bit.Core.Services
{
    public class TwoFactorService : ITwoFactorService
    {
        private readonly II18nService _i18nService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private Dictionary<TwoFactorProviderType, TwoFactorProvider> _twoFactorProviders;
        private TwoFactorProviderType? _selectedTwoFactorProviderType;
        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> _twoFactorProvidersData;

        public TwoFactorService(
            II18nService i18nService,
            IPlatformUtilsService platformUtilsService
        )
        {
            _i18nService = i18nService;
            _platformUtilsService = platformUtilsService;

            _twoFactorProviders = new Dictionary<TwoFactorProviderType, TwoFactorProvider>();
            _twoFactorProviders.Add(TwoFactorProviderType.Authenticator, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Authenticator,
                Priority = 1,
                Sort = 1
            });
            _twoFactorProviders.Add(TwoFactorProviderType.YubiKey, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.YubiKey,
                Priority = 3,
                Sort = 2,
                Premium = true
            });
            _twoFactorProviders.Add(TwoFactorProviderType.Duo, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Duo,
                Name = "Duo",
                Priority = 2,
                Sort = 3,
                Premium = true
            });
            _twoFactorProviders.Add(TwoFactorProviderType.OrganizationDuo, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.OrganizationDuo,
                Name = "Duo (Organization)",
                Priority = 10,
                Sort = 4
            });
            _twoFactorProviders.Add(TwoFactorProviderType.Fido2WebAuthn, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Fido2WebAuthn,
                Priority = 4,
                Sort = 5,
                Premium = true
            });
            _twoFactorProviders.Add(TwoFactorProviderType.Email, new TwoFactorProvider
            {
                Type = TwoFactorProviderType.Email,
                Priority = 0,
                Sort = 6,
            });
        }

        public void Init()
        {
            _twoFactorProviders[TwoFactorProviderType.Email].Name = _i18nService.T("Email");
            _twoFactorProviders[TwoFactorProviderType.Email].Description = _i18nService.T("EmailDesc");
            _twoFactorProviders[TwoFactorProviderType.Authenticator].Name = _i18nService.T("AuthenticatorAppTitle");
            _twoFactorProviders[TwoFactorProviderType.Authenticator].Description =
                _i18nService.T("AuthenticatorAppDesc");
            _twoFactorProviders[TwoFactorProviderType.Duo].Description = _i18nService.T("DuoDesc");
            _twoFactorProviders[TwoFactorProviderType.OrganizationDuo].Name =
                string.Format("Duo ({0})", _i18nService.T("Organization"));
            _twoFactorProviders[TwoFactorProviderType.OrganizationDuo].Description =
                _i18nService.T("DuoOrganizationDesc");
            _twoFactorProviders[TwoFactorProviderType.Fido2WebAuthn].Name = _i18nService.T("Fido2Title");
            _twoFactorProviders[TwoFactorProviderType.Fido2WebAuthn].Description = _i18nService.T("Fido2Desc");
            _twoFactorProviders[TwoFactorProviderType.YubiKey].Name = _i18nService.T("YubiKeyTitle");
            _twoFactorProviders[TwoFactorProviderType.YubiKey].Description = _i18nService.T("YubiKeyDesc");
        }

        public List<TwoFactorProvider> GetSupportedProviders()
        {
            var providers = new List<TwoFactorProvider>();
            if (_twoFactorProvidersData == null)
            {
                return providers;
            }
            if (_twoFactorProvidersData.ContainsKey(TwoFactorProviderType.OrganizationDuo) &&
                _platformUtilsService.SupportsDuo())
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.OrganizationDuo]);
            }
            if (_twoFactorProvidersData.ContainsKey(TwoFactorProviderType.Authenticator))
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Authenticator]);
            }
            if (_twoFactorProvidersData.ContainsKey(TwoFactorProviderType.YubiKey))
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.YubiKey]);
            }
            if (_twoFactorProvidersData.ContainsKey(TwoFactorProviderType.Duo) && _platformUtilsService.SupportsDuo())
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Duo]);
            }
            if (_twoFactorProvidersData.ContainsKey(TwoFactorProviderType.Fido2WebAuthn) &&
                _platformUtilsService.SupportsFido2())
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Fido2WebAuthn]);
            }
            if (_twoFactorProvidersData.ContainsKey(TwoFactorProviderType.Email))
            {
                providers.Add(_twoFactorProviders[TwoFactorProviderType.Email]);
            }
            return providers;
        }

        public TwoFactorProviderType? GetDefaultProvider(bool fido2Supported)
        {
            if (_twoFactorProvidersData == null)
            {
                return null;
            }
            if (_selectedTwoFactorProviderType != null &&
                _twoFactorProvidersData.ContainsKey(_selectedTwoFactorProviderType.Value))
            {
                return _selectedTwoFactorProviderType.Value;
            }
            TwoFactorProviderType? providerType = null;
            var providerPriority = -1;
            foreach (var providerKvp in _twoFactorProvidersData)
            {
                if (_twoFactorProviders.ContainsKey(providerKvp.Key))
                {
                    var provider = _twoFactorProviders[providerKvp.Key];
                    if (provider.Priority > providerPriority)
                    {
                        if (providerKvp.Key == TwoFactorProviderType.Fido2WebAuthn && !fido2Supported)
                        {
                            continue;
                        }
                        providerType = providerKvp.Key;
                        providerPriority = provider.Priority;
                    }
                }
            }
            return providerType;
        }

        public void SetSelectedProvider(TwoFactorProviderType type)
        {
            _selectedTwoFactorProviderType = type;
        }

        public void ClearSelectedProvider()
        {
            _selectedTwoFactorProviderType = null;
        }

        public void SetProviders(IdentityTwoFactorResponse response)
        {
            _twoFactorProvidersData = response.TwoFactorProviders2;
        }
        public void ClearProviders()
        {
            _twoFactorProvidersData = null;
        }

        public Dictionary<TwoFactorProviderType, Dictionary<string, object>> GetProviders()
        {
            return _twoFactorProvidersData;
        }
    }
}
