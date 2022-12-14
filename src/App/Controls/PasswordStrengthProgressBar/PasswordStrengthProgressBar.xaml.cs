using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class PasswordStrengthProgressBar : StackLayout
    {
        private readonly PasswordStrengthViewModel _viewModel;

        public static readonly BindableProperty PasswordStrengthProperty = BindableProperty.Create(
            nameof(PasswordStrength),
            typeof(PasswordStrengthValue),
            typeof(PasswordStrengthProgressBar),
            defaultBindingMode: BindingMode.OneWayToSource);

        public static readonly BindableProperty EmailProperty = BindableProperty.Create(
            nameof(Email),
            typeof(string),
            typeof(PasswordStrengthProgressBar),
            string.Empty);

        public static readonly BindableProperty PasswordProperty = BindableProperty.Create(
            nameof(Password),
            typeof(string),
            typeof(PasswordStrengthProgressBar),
            string.Empty);

        public PasswordStrengthValue PasswordStrength
        {
            get { return (PasswordStrengthValue)GetValue(PasswordStrengthProperty); }
            set { SetValue(PasswordStrengthProperty, value); }
        }

        public string Email
        {
            get { return (string)GetValue(EmailProperty); }
            set { SetValue(EmailProperty, value); }
        }

        public string Password
        {
            get { return (string)GetValue(PasswordProperty); }
            set { SetValue(PasswordProperty, value); }
        }

        public PasswordStrengthProgressBar()
        {
            InitializeComponent();
            _viewModel = new PasswordStrengthViewModel();
            _passwordBar.BindingContext = _viewModel;
            _passwordStatus.BindingContext = _viewModel;
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == PasswordProperty.PropertyName ||
                propertyName == EmailProperty.PropertyName)
            {
                _viewModel.CalculateMasterPasswordStrength(Password, Email);
                _passwordBar.ProgressTo(_viewModel.PasswordStrength, 500, Easing.SinOut);
                PasswordStrength = _viewModel.PasswordStrengthEnum;
            }
        }
    }
}

