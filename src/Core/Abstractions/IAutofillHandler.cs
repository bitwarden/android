using Bit.Core.Models.View;

namespace Bit.Core.Abstractions
{
    public interface IAutofillHandler
    {
        bool AutofillServicesEnabled();
        bool SupportsAutofillService();
        void Autofill(CipherView cipher);
        void CloseAutofill();
        bool AutofillAccessibilityServiceRunning();
        bool AutofillAccessibilityOverlayPermitted();
        bool AutofillServiceEnabled();
        void DisableAutofillService();
    }
}
