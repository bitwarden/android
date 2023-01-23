using System.Collections.Generic;

namespace Bit.App.Controls
{
    public interface IPasswordStrengthable
    {
        string Password { get; }
        List<string> UserInputs { get; }
    }
}

