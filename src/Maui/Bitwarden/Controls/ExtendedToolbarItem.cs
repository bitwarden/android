using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class ExtendedToolbarItem : ToolbarItem
    {
        public bool UseOriginalImage { get; set; }

        // HACK: For the issue of correctly updating the avatar toolbar item color on iOS
        // we need to subscribe to the PropertyChanged event of the ToolbarItem on the CustomNavigationRenderer
        // The problem is that there are a lot of private places where the navigation renderer disposes objects
        // that we don't have access to, and that we should in order to properly prevent memory leaks
        // So as a hack solution we have this OnAppearing/OnDisappearing actions and methods to be called on page lifecycle
        // to subscribe/unsubscribe indirectly on the CustomNavigationRenderer
        public Action OnAppearingAction { get; set; }
        public Action OnDisappearingAction { get; set; }

        public void OnAppearing()
        {
            OnAppearingAction?.Invoke();
        }

        public void OnDisappearing()
        {
            OnDisappearingAction?.Invoke();
        }
    }
}
