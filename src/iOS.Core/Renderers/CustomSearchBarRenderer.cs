using Bit.iOS.Core.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(SearchBar), typeof(CustomSearchBarRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class CustomSearchBarRenderer : SearchBarRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<SearchBar> e)
        {
            base.OnElementChanged(e);
            if (e.NewElement is SearchBar)
            {
                UpdateKeyboardAppearance();
            }
        }

        private void UpdateKeyboardAppearance()
        {
            if (!Utilities.ThemeHelpers.LightTheme)
            {
                Control.KeyboardAppearance = UIKeyboardAppearance.Dark;
            }
        }
    }
}
