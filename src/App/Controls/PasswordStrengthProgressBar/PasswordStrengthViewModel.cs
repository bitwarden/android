using System;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using Bit.Core.Abstractions;
using Bit.Core.Attributes;
using Bit.Core.Models.Data;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class PasswordStrengthViewModel : ExtendedViewModel
    {
        private readonly IPasswordGenerationService _passwordGenerationService;
        private double _passwordStrength;
        private Color _passwordColor;
        private PasswordStrengthCategory _passwordStrengthCategory;

        public PasswordStrengthViewModel()
        {
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>();
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

        public PasswordStrengthCategory PasswordStrengthCategory
        {
            get => _passwordStrengthCategory;
            set => SetProperty(ref _passwordStrengthCategory, value);
        }

        public void CalculateMasterPasswordStrength(string password, string email)
        {
            if (string.IsNullOrEmpty(password))
            {
                PasswordStrength = 0;
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStrengthCategory = PasswordStrengthCategory.None;
                return;
            }

            var passwordStrength = _passwordGenerationService.PasswordStrength(password, email);
            PasswordStrength = (passwordStrength.Score + 1f) / 5f;
            if (PasswordStrength <= 0.4f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStrengthCategory = PasswordStrengthCategory.Weak;
            }
            else if (PasswordStrength <= 0.6f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("WarningColor");
                PasswordStrengthCategory = PasswordStrengthCategory.Weak;
            }
            else if (PasswordStrength <= 0.8f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("PrimaryColor");
                PasswordStrengthCategory = PasswordStrengthCategory.Good;
            }
            else if (PasswordStrength <= 1f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("SuccessColor");
                PasswordStrengthCategory = PasswordStrengthCategory.Strong;
            }
        }
    }

    public enum PasswordStrengthCategory
    {
        [LocalizableEnum(" ")]
        None,
        [LocalizableEnum("Weak")]
        Weak,
        [LocalizableEnum("Good")]
        Good,
        [LocalizableEnum("Strong")]
        Strong
    }
}

