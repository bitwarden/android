using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface ITwoFactorService
    {
        void Init();
        List<TwoFactorProvider> GetSupportedProviders();
        TwoFactorProviderType? GetDefaultProvider(bool fido2Supported);

        void SetSelectedProvider(TwoFactorProviderType type);
        void ClearSelectedProvider();

        void SetProviders(IdentityTwoFactorResponse response);
        void ClearProviders();
        Dictionary<TwoFactorProviderType, Dictionary<string, object>> GetProviders();
    }
}
