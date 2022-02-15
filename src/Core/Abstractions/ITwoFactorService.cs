using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface ITwoFactorService
    {
        void Init();


        TwoFactorProviderType? SelectedTwoFactorProviderType { get; set; }
        Dictionary<TwoFactorProviderType, TwoFactorProvider> TwoFactorProviders { get; set; }
        Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProvidersData { get; set; }

        TwoFactorProviderType? GetDefaultTwoFactorProvider(bool fido2Supported);
    }
}
