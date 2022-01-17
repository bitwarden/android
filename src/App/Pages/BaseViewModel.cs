using Bit.App.Controls;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public abstract class BaseViewModel : ExtendedViewModel
    {
        private string _pageTitle = string.Empty;
        private AvatarImageSource _avatar;

        public string PageTitle
        {
            get => _pageTitle;
            set => SetProperty(ref _pageTitle, value);
        }

        public AvatarImageSource AvatarImageSource
        {
            get => _avatar ?? new AvatarImageSource();
            set => SetProperty(ref _avatar, value);
        }

        public ContentPage Page { get; set; }
    }
}
