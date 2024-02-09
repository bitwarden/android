using Bit.App.Abstractions;

namespace Bit.App.Utilities.AccountManagement
{
    public class LockNavigationParams : INavigationParams
    {
        public LockNavigationParams(bool autoPromptBiometric = true)
        {
            AutoPromptBiometric = autoPromptBiometric;
        }

        public bool AutoPromptBiometric { get; }
    }
}
