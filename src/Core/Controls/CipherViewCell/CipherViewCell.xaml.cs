using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public partial class CipherViewCell : BaseCipherViewCell
    {
        private const int ICON_COLUMN_DEFAULT_WIDTH = 40;
        private const int ICON_IMAGE_DEFAULT_WIDTH = 22;

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

        protected override CachedImage Icon => _iconImage;

        protected override IconLabel IconPlaceholder => _iconPlaceholderImage;

        public ICommand ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as ICommand;
            set => SetValue(ButtonCommandProperty, value);
        }

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            if (BindingContext is CipherItemViewModel cipherItem)
            {
                ButtonCommand?.Execute(cipherItem.Cipher);
            }
        }
    }
}
