using System;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;

namespace Bit.iOS.Core.Services
{
    /// <summary>
    /// This handler is only needed on Android for now, now this class acts as a stub so that dependency injection doesn't break
    /// </summary>
    public class AutofillHandler : IAutofillHandler
    {
        public bool SupportsAutofillService() => false;
        public bool AutofillServiceEnabled() => false;
        public void DisableCredentialProviderService() => throw new NotImplementedException();
        public void Autofill(CipherView cipher) => throw new NotImplementedException();
        public bool AutofillAccessibilityOverlayPermitted() => false;
        public bool AutofillAccessibilityServiceRunning() => false;
        public bool CredentialProviderServiceEnabled() => throw new NotImplementedException();
        public bool AutofillServicesEnabled() => false;
        public void CloseAutofill() => throw new NotImplementedException();
        public void DisableAutofillService() => throw new NotImplementedException();
    }
}

