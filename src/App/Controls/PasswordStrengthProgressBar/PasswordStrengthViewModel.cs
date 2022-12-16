using System.Collections.Generic;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class PasswordStrengthViewModel : ExtendedViewModel
    {
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly IPasswordStrengthable _passwordStrengthable;
        private double _passwordStrength;
        private Color _passwordColor;
        private PasswordStrengthLevel? _passwordStrengthLevel;

        public PasswordStrengthViewModel(IPasswordStrengthable passwordStrengthable)
        {
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>();
            _passwordStrengthable = passwordStrengthable;
        }

        public double PasswordStrength
        {
            get => _passwordStrength;
            set => SetProperty(ref _passwordStrength, value);
        }

        public Color PasswordColor
        {
            get => _passwordColor;
            set => SetProperty(ref _passwordColor, value);
        }

        public PasswordStrengthLevel? PasswordStrengthLevel
        {
            get => _passwordStrengthLevel;
            set => SetProperty(ref _passwordStrengthLevel, value);
        }

        public List<string> GetPasswordStrengthUserInput(string email) => _passwordGenerationService.GetPasswordStrengthUserInput(email);

        public void CalculatePasswordStrength()
        {
            if (string.IsNullOrEmpty(_passwordStrengthable.Password))
            {
                PasswordStrength = 0;
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStrengthLevel = null;
                return;
            }

            var passwordStrength = _passwordGenerationService.PasswordStrength(_passwordStrengthable.Password, _passwordStrengthable.UserInputs);
            // The passwordStrength.Score is 0..4, convertion was made to be used as a progress directly by the control 0..1 
            PasswordStrength = (passwordStrength.Score + 1f) / 5f;
            if (PasswordStrength <= 0.4f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStrengthLevel = Controls.PasswordStrengthLevel.Weak;
            }
            else if (PasswordStrength <= 0.6f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("WarningColor");
                PasswordStrengthLevel = Controls.PasswordStrengthLevel.Weak;
            }
            else if (PasswordStrength <= 0.8f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("PrimaryColor");
                PasswordStrengthLevel = Controls.PasswordStrengthLevel.Good;
            }
            else
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("SuccessColor");
                PasswordStrengthLevel = Controls.PasswordStrengthLevel.Strong;
            }
        }
    }
}

