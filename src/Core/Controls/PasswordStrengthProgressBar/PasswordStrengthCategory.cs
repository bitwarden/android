using Bit.Core.Attributes;

namespace Bit.App.Controls
{
    public enum PasswordStrengthLevel
    {
        [LocalizableEnum("Weak")]
        VeryWeak,
        [LocalizableEnum("Weak")]
        Weak,
        [LocalizableEnum("Good")]
        Good,
        [LocalizableEnum("Strong")]
        Strong
    }
}

