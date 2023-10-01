using AndroidX.AppCompat.View.Menu;
using AndroidX.Navigation.UI;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Google.Android.Material.BottomNavigation;
using Microsoft.Maui.Handlers;

namespace Bit.App.Handlers
{
    public partial class CustomTabbedPageHandler : TabbedViewHandler
    {
        private TabbedPage _tabbedPage;
        private BottomNavigationView _bottomNavigationView;
        private Android.Views.ViewGroup _bottomNavigationViewGroup;
        private ILogger _logger;

        protected override void ConnectHandler(global::Android.Views.View platformView) 
        {
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            if(VirtualView is TabbedPage tabbedPage)
            {
                _tabbedPage = tabbedPage;
                _tabbedPage.Loaded += TabbedPage_Loaded;
            }

            base.ConnectHandler(platformView);
        }

        private void TabbedPage_Loaded(object sender, EventArgs e)
        {
            try
            {
                //This layout should always be the same/fixed and therefore this should run with no issues. Nevertheless it's wrapped in try catch to avoid crashing in edge-case scenarios.
                _bottomNavigationViewGroup = (((sender as VisualElement).Handler as IPlatformViewHandler)
                        .PlatformView
                        .Parent
                        .Parent as Android.Views.View)
                    .FindViewById(Microsoft.Maui.Controls.Resource.Id.navigationlayout_bottomtabs) as Android.Views.ViewGroup;
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }

            if(_bottomNavigationViewGroup == null) { return; }

            //If TabbedPage still doesn't have items we set an event to wait for them
			if (_bottomNavigationViewGroup.ChildCount == 0)
			{
				_bottomNavigationViewGroup.ChildViewAdded += View_ChildViewAdded;
			}
			else
			{ //If we already have items we can start listening immediately
				var bottomTabs = _bottomNavigationViewGroup.GetChildAt(0);
                ListenToItemReselected(bottomTabs);
			}
        }

        private void ListenToItemReselected(Android.Views.View bottomTabs)
        {
            if(bottomTabs is BottomNavigationView bottomNavigationView)
			{
                //If there was an older _bottomNavigationView for some reason we want to make sure to unregister
                if(_bottomNavigationView != null)
                {
                    _bottomNavigationView.ItemReselected -= BottomNavigationView_ItemReselected;
                    _bottomNavigationView = null;
                }

                _bottomNavigationView = bottomNavigationView;
                _bottomNavigationView.LabelVisibilityMode = LabelVisibilityMode.LabelVisibilityLabeled;
                _bottomNavigationView.ItemReselected += BottomNavigationView_ItemReselected;
			}
        }

        private void View_ChildViewAdded(object sender, Android.Views.ViewGroup.ChildViewAddedEventArgs e)
		{
            //We shouldn't need this to be called anymore times so we can unregister to the events now
            if(_bottomNavigationViewGroup != null)
            {
                _bottomNavigationViewGroup.ChildViewAdded -= View_ChildViewAdded;
            }

			var bottomTabs = e.Child;
            ListenToItemReselected(bottomTabs);
		}

        private void BottomNavigationView_ItemReselected(object sender, Google.Android.Material.Navigation.NavigationBarView.ItemReselectedEventArgs e)
        {
			if(e.Item is MenuItemImpl item)
			{
				System.Diagnostics.Debug.WriteLine($"Tab '{item.Title}' was reselected so we'll PopToRoot.");
                MainThread.BeginInvokeOnMainThread(async () => await _tabbedPage.CurrentPage.Navigation.PopToRootAsync());
			}
        }

        protected override void DisconnectHandler(global::Android.Views.View platformView) 
        {
            if(_bottomNavigationViewGroup != null)
            {
                _bottomNavigationViewGroup.ChildViewAdded -= View_ChildViewAdded;
                _bottomNavigationViewGroup = null;
            }

            if(_bottomNavigationView != null)
            {
                _bottomNavigationView.ItemReselected -= BottomNavigationView_ItemReselected;
                _bottomNavigationView = null;
            }

            if(_tabbedPage != null)
            {
                _tabbedPage.Loaded -= TabbedPage_Loaded;
                _tabbedPage = null;
            }

            _logger = null;

            base.DisconnectHandler(platformView);
        }
    }
}
