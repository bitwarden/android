using System.Runtime.CompilerServices;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class PasswordStrengthProgressBar : StackLayout
    {
        private PasswordStrengthViewModel ViewModel => BindingContext as PasswordStrengthViewModel;

        public static readonly BindableProperty VeryWeakColorProperty = BindableProperty.Create(
            nameof(VeryWeakColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            Utilities.ThemeManager.GetResourceColor("DangerColor"));

        public static readonly BindableProperty WeakColorProperty = BindableProperty.Create(
            nameof(WeakColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            Utilities.ThemeManager.GetResourceColor("WarningColor"));

        public static readonly BindableProperty GoodColorProperty = BindableProperty.Create(
            nameof(GoodColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            Utilities.ThemeManager.GetResourceColor("PrimaryColor"));

        public static readonly BindableProperty StrongColorProperty = BindableProperty.Create(
            nameof(StrongColor),
            typeof(Color),
            typeof(PasswordStrengthProgressBar),
            Utilities.ThemeManager.GetResourceColor("SuccessColor"));

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
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();
            UpdateColors();
        }

        protected override void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == nameof(VeryWeakColor)
                || propertyName == nameof(WeakColor)
                || propertyName == nameof(GoodColor)
                || propertyName == nameof(StrongColor))
            {
                UpdateColors();
                ViewModel.CalculatePasswordStrength();
            }
        }

        private void UpdateColors()
        {
            ViewModel.VeryWeakColor = VeryWeakColor;
            ViewModel.WeakColor = WeakColor;
            ViewModel.GoodColor = GoodColor;
            ViewModel.StrongColor = StrongColor;
        }
    }
}

