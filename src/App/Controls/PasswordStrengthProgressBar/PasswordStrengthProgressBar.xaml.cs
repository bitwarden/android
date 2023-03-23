using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class PasswordStrengthProgressBar : StackLayout
    {
        public static readonly BindableProperty PasswordStrengthLevelProperty = BindableProperty.Create(
            nameof(PasswordStrengthLevel),
            typeof(PasswordStrengthLevel),
            typeof(PasswordStrengthProgressBar),
            propertyChanged: OnControlPropertyChanged);

        public static readonly BindableProperty VeryWeakColorProperty = BindableProperty.Create(
            nameof(VeryWeakColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            propertyChanged: OnControlPropertyChanged);

        public static readonly BindableProperty WeakColorProperty = BindableProperty.Create(
            nameof(WeakColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            propertyChanged: OnControlPropertyChanged);

        public static readonly BindableProperty GoodColorProperty = BindableProperty.Create(
            nameof(GoodColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            propertyChanged: OnControlPropertyChanged);

        public static readonly BindableProperty StrongColorProperty = BindableProperty.Create(
            nameof(StrongColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            propertyChanged: OnControlPropertyChanged);

        public PasswordStrengthLevel? PasswordStrengthLevel
        {
            get { return (PasswordStrengthLevel?)GetValue(PasswordStrengthLevelProperty); }
            set { SetValue(PasswordStrengthLevelProperty, value); }
        }

        public Color VeryWeakColor
        {
            get { return (Color)GetValue(VeryWeakColorProperty); }
            set { SetValue(VeryWeakColorProperty, value); }
        }

        public Color WeakColor
        {
            get { return (Color)GetValue(WeakColorProperty); }
            set { SetValue(WeakColorProperty, value); }
        }

        public Color GoodColor
        {
            get { return (Color)GetValue(GoodColorProperty); }
            set { SetValue(GoodColorProperty, value); }
        }

        public Color StrongColor
        {
            get { return (Color)GetValue(StrongColorProperty); }
            set { SetValue(StrongColorProperty, value); }
        }

        public PasswordStrengthProgressBar()
        {
            InitializeComponent();
            SetBinding(PasswordStrengthProgressBar.PasswordStrengthLevelProperty, new Binding() { Path = nameof(PasswordStrengthViewModel.PasswordStrengthLevel) });
            UpdateColors();
        }

        private static void OnControlPropertyChanged(BindableObject bindable, object oldValue, object newValue)
        {
            (bindable as PasswordStrengthProgressBar)?.UpdateColors();
        }

        public void UpdateColors()
        {
            if (_progressBar == null || _progressLabel == null)
            {
                return;
            }
            _progressBar.ProgressColor = GetColorForStrength();
            _progressLabel.TextColor = _progressBar.ProgressColor;
        }

        private Color GetColorForStrength()
        {
            switch (PasswordStrengthLevel)
            {
                case Controls.PasswordStrengthLevel.VeryWeak:
                    return VeryWeakColor;
                case Controls.PasswordStrengthLevel.Weak:
                    return WeakColor;
                case Controls.PasswordStrengthLevel.Good:
                    return GoodColor;
                case Controls.PasswordStrengthLevel.Strong:
                    return StrongColor;
                default:
                    return Color.Transparent;
            }
        }
    }
}

