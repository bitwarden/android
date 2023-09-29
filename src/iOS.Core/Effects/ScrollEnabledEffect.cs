// TODO: [MAUI-Migration] Check if moving this to the main project works for extensions.

//using Bit.iOS.Core.Effects;
//using UIKit;
//using Xamarin.Forms;
//using Xamarin.Forms.Platform.iOS;

//[assembly: ResolutionGroupName("Bitwarden")]
//[assembly: ExportEffect(typeof(ScrollEnabledEffect), "ScrollEnabledEffect")]
//namespace Bit.iOS.Core.Effects
//{
//    public class ScrollEnabledEffect : PlatformEffect
//    {
//        protected override void OnAttached()
//        {
//            // this can be for any view that inherits from UIScrollView like UITextView.
//            if (Element != null && Control is UIScrollView scrollView)
//            {
//                scrollView.ScrollEnabled = App.Effects.ScrollEnabledEffect.GetIsScrollEnabled(Element);
//            }
//        }

//        protected override void OnDetached()
//        {
//        }
//    }
//}
