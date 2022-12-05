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
        public void Autofill(CipherView cipher) => throw new NotImplementedException();
        public bool AutofillAccessibilityOverlayPermitted() => throw new NotImplementedException();
        public bool AutofillAccessibilityServiceRunning() => throw new NotImplementedException();
        public bool AutofillServicesEnabled() => throw new NotImplementedException();
        public void CloseAutofill() => throw new NotImplementedException();
        public void DisableAutofillService() => throw new NotImplementedException();
    }
}

