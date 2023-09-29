// TODO: [MAUI-Migration] Check if moving this to the main project works for extensions.

//using Bit.iOS.Core.Effects;
//using UIKit;
//using Xamarin.Forms;
//using Xamarin.Forms.Platform.iOS;

//[assembly: ExportEffect(typeof(ScrollViewContentInsetAdjustmentBehaviorEffect), nameof(ScrollViewContentInsetAdjustmentBehaviorEffect))]
//namespace Bit.iOS.Core.Effects
//{
//    public class ScrollViewContentInsetAdjustmentBehaviorEffect : PlatformEffect
//    {
//        protected override void OnAttached()
//        {
//            if (Element != null && Control is UIScrollView scrollView)
//            {
//                switch (App.Effects.ScrollViewContentInsetAdjustmentBehaviorEffect.GetContentInsetAdjustmentBehavior(Element))
//                {
//                    case App.Effects.ScrollContentInsetAdjustmentBehavior.Automatic:
//                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Automatic;
//                        break;
//                    case App.Effects.ScrollContentInsetAdjustmentBehavior.ScrollableAxes:
//                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.ScrollableAxes;
//                        break;
//                    case App.Effects.ScrollContentInsetAdjustmentBehavior.Never:
//                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Never;
//                        break;
//                    case App.Effects.ScrollContentInsetAdjustmentBehavior.Always:
//                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Always;
//                        break;
//                }
//            }
//        }

//        protected override void OnDetached()
//        {
//        }
//    }
//}
