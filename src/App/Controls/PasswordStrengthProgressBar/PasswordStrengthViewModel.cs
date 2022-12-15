using System;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using Bit.Core.Abstractions;
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
        private string _passwordStatus;

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

        public string PasswordStatus
        {
            get => _passwordStatus;
            set => SetProperty(ref _passwordStatus, value);
        }

        public PasswordStrengthCategory PasswordStrengthCategory { get; set; }

        public void CalculateMasterPasswordStrength(string password, string email)
        {
            if (string.IsNullOrEmpty(password))
            {
                PasswordStrength = 0;
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStatus = " ";
                PasswordStrengthCategory = PasswordStrengthCategory.Weak;
                return;
            }

            var passwordStrength = _passwordGenerationService.PasswordStrength(password, email);
            PasswordStrength = (passwordStrength.Score + 1f) / 5f;
            if (PasswordStrength <= 0.2f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStatus = Resources.AppResources.Weak;
                PasswordStrengthCategory = PasswordStrengthCategory.Weak;
            }
            else if (PasswordStrength <= 0.4f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("DangerColor");
                PasswordStatus = Resources.AppResources.Weak;
                PasswordStrengthCategory = PasswordStrengthCategory.Weak;
            }
            else if (PasswordStrength <= 0.6f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("WarningColor");
                PasswordStatus = Resources.AppResources.Weak;
                PasswordStrengthCategory = PasswordStrengthCategory.Weak;
            }
            else if (PasswordStrength <= 0.8f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("PrimaryColor");
                PasswordStatus = Resources.AppResources.Good;
                PasswordStrengthCategory = PasswordStrengthCategory.Good;
            }
            else if (PasswordStrength <= 1f)
            {
                PasswordColor = Utilities.ThemeManager.GetResourceColor("SuccessColor");
                PasswordStatus = Resources.AppResources.Strong;
                PasswordStrengthCategory = PasswordStrengthCategory.Strong;
            }
        }
    }

    public enum PasswordStrengthCategory
    {
        Weak,
        Good,
        Strong
    }
}

