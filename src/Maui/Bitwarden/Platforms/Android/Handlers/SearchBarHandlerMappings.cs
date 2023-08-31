using Android.Views.InputMethods;

namespace Bit.App.Handlers
{
    public partial class SearchBarHandlerMappings
    {
        partial void SetupPlatform()
        {
            Microsoft.Maui.Handlers.SearchBarHandler.Mapper.AppendToMapping("CustomSearchBarHandler", (handler, searchBar) =>
            {
                try
                {
                    var magId = handler.PlatformView.Resources.GetIdentifier("android:id/search_mag_icon", null, null);
                    var magImage = (Android.Widget.ImageView)handler.PlatformView.FindViewById(magId);
                    magImage.LayoutParameters = new Android.Widget.LinearLayout.LayoutParams(0, 0);
                }
                catch { }
                // TODO: [MAUI-Migration] [Check]
                handler.PlatformView.ImeOptions = handler.PlatformView.ImeOptions | (int)ImeFlags.NoPersonalizedLearning |
                    (int)ImeFlags.NoExtractUi;
                //Control.SetImeOptions(Control.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                //    (ImeAction)ImeFlags.NoExtractUi);
            });
        }
    }
}
