using Android.Content;
using Android.Views;
using Bit.Droid.Renderers;
using Google.Android.Material.BottomNavigation;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Xamarin.Forms.Platform.Android.AppCompat;

[assembly: ExportRenderer(typeof(TabbedPage), typeof(CustomTabbedRenderer))]
namespace Bit.Droid.Renderers
{
    public class CustomTabbedRenderer : TabbedPageRenderer, BottomNavigationView.IOnNavigationItemReselectedListener
    {
        private TabbedPage _page;

        public CustomTabbedRenderer(Context context) : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<TabbedPage> e)
        {
            base.OnElementChanged(e);
            if (e.NewElement != null)
            {
                _page = e.NewElement;
                GetBottomNavigationView()?.SetOnNavigationItemReselectedListener(this);
            }
            else
            {
                _page = e.OldElement;
            }
        }

        private BottomNavigationView GetBottomNavigationView()
        {
            for (var i = 0; i < ViewGroup.ChildCount; i++)
            {
                var childView = ViewGroup.GetChildAt(i);
                if (childView is ViewGroup viewGroup)
                {
                    for (var j = 0; j < viewGroup.ChildCount; j++)
                    {
                        var childRelativeLayoutView = viewGroup.GetChildAt(j);
                        if (childRelativeLayoutView is BottomNavigationView bottomNavigationView)
                        {
                            return bottomNavigationView;
                        }
                    }
                }
            }
            return null;
        }

        async void BottomNavigationView.IOnNavigationItemReselectedListener.OnNavigationItemReselected(IMenuItem item)
        {
            if (_page?.CurrentPage?.Navigation != null && _page.CurrentPage.Navigation.NavigationStack.Count > 0)
            {
                await _page.CurrentPage.Navigation.PopToRootAsync();
            }
        }
    }
}
