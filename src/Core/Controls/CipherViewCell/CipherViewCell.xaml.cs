using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public partial class CipherViewCell : ExtendedGrid
    {
        private const int ICON_COLUMN_DEFAULT_WIDTH = 40;
        private const int ICON_IMAGE_DEFAULT_WIDTH = 22;

        public static readonly BindableProperty CipherProperty = BindableProperty.Create(
            nameof(Cipher), typeof(CipherView), typeof(CipherViewCell), default(CipherView), BindingMode.OneWay);

        public static readonly BindableProperty WebsiteIconsEnabledProperty = BindableProperty.Create(
            nameof(WebsiteIconsEnabled), typeof(bool?), typeof(CipherViewCell));

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(ICommand), typeof(CipherViewCell));

        public CipherViewCell()
        {
            InitializeComponent();

            var fontScale = ServiceContainer.Resolve<IDeviceActionService>().GetSystemFontSizeScale();
            _iconColumn.Width = new GridLength(ICON_COLUMN_DEFAULT_WIDTH * fontScale, GridUnitType.Absolute);
            _iconImage.WidthRequest = ICON_IMAGE_DEFAULT_WIDTH * fontScale;
            _iconImage.HeightRequest = ICON_IMAGE_DEFAULT_WIDTH * fontScale;
        }

        public bool? WebsiteIconsEnabled
        {
            get => (bool)GetValue(WebsiteIconsEnabledProperty);
            set => SetValue(WebsiteIconsEnabledProperty, value);
        }

        public CipherView Cipher
        {
            get => GetValue(CipherProperty) as CipherView;
            set => SetValue(CipherProperty, value);
        }

        public ICommand ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as ICommand;
            set => SetValue(ButtonCommandProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (BindingContext is CipherViewCellViewModel cipherViewCellViewModel && propertyName == WebsiteIconsEnabledProperty.PropertyName)
            {
                cipherViewCellViewModel.WebsiteIconsEnabled = WebsiteIconsEnabled ?? false;
            }
        }

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            // WORKAROUND: Added a temporary workaround so that the MoreButton still works in all pages even if it uses GroupingsPageListItem instead of CipherViewCellViewModel.
            // Ideally this should be fixed so that even Groupings Page uses CipherViewCellViewModel
            CipherView cipherView = null;
            if (BindingContext is CipherViewCellViewModel cipherViewCellViewModel)
            {
                cipherView = cipherViewCellViewModel.Cipher;
            }
            else if (BindingContext is GroupingsPageListItem groupingsPageListItem)
            {
                cipherView = groupingsPageListItem.Cipher;
            }

            if (cipherView != null)
            {
                ButtonCommand?.Execute(cipherView);
            }
        }
    }
}
