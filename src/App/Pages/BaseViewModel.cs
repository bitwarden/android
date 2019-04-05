using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public abstract class BaseViewModel : ExtendedViewModel
    {
        private string _pageTitle = string.Empty;

        public string PageTitle
        {
            get => _pageTitle;
            set => SetProperty(ref _pageTitle, value);
        }

        public ContentPage Page { get; set; }
    }
}
