using Bit.App.Utilities;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class CipherItemViewModel : ExtendedViewModel, IGroupingsPageListItem
    {
        private readonly bool _websiteIconsEnabled;
        private UriImageSource _iconImageSource = null;

        public CipherItemViewModel(CipherView cipherView, bool websiteIconsEnabled, bool fuzzyAutofill = false)
        {
            Cipher = cipherView;
            _websiteIconsEnabled = websiteIconsEnabled;
            FuzzyAutofill = fuzzyAutofill;
        }

        public CipherView Cipher { get; }

        public bool FuzzyAutofill { get; }

        public bool ShowIconImage
        {
            get => _websiteIconsEnabled
                && !string.IsNullOrWhiteSpace(Cipher.LaunchUri)
                && IconImageSource != null;
        }

        public UriImageSource IconImageSource
        {
            get
            {
                if (_iconImageSource == null) // default value since icon source can return null
                {
                    var iconImageStr = IconImageHelper.GetIconImage(Cipher);
                    if (string.IsNullOrWhiteSpace(iconImageStr)) { return null; }

                    _iconImageSource = new UriImageSource
                    {
                        Uri = new Uri(iconImageStr),
                        CacheValidity = TimeSpan.FromDays(90),
                        CachingEnabled = true
                    };
                }

                return _iconImageSource;
            }
        }

        /// <summary>
        /// Flag that indicates if FFImageLoading has successfully finished loading  the image.
        /// This is useful to check when the cell is being reused.
        /// </summary>
        public bool IconImageSuccesfullyLoaded { get; set; }

        public bool UsePasskeyIconAsPlaceholderFallback { get; set; }
    }
}
