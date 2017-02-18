using System;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Android.Support.Design.Widget;
using Xamarin.Forms.Platform.Android.AppCompat;
using System.Reflection;
using System.Linq;

[assembly: ExportRenderer(typeof(ExtendedTabbedPage), typeof(ExtendedTabbedPageRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedTabbedPageRenderer : TabbedPageRenderer, TabLayout.IOnTabSelectedListener
    {
        private TabLayout _tabLayout;

        protected override void OnElementChanged(ElementChangedEventArgs<TabbedPage> e)
        {
            base.OnElementChanged(e);
            var view = (ExtendedTabbedPage)Element;

            var field = typeof(ExtendedTabbedPageRenderer).BaseType.GetField("_tabLayout",
                BindingFlags.Instance | BindingFlags.NonPublic);
            _tabLayout = field?.GetValue(this) as TabLayout;
            if(_tabLayout != null)
            {
                var tab = _tabLayout.GetTabAt(0);
                SetSelectedTabIcon(tab, Element.Children[0].Icon, true);
            }
        }

        void TabLayout.IOnTabSelectedListener.OnTabSelected(TabLayout.Tab tab)
        {
            if(Element == null)
            {
                return;
            }

            var selectedIndex = tab.Position;
            var child = Element.Children[selectedIndex];
            if(Element.Children.Count > selectedIndex && selectedIndex >= 0)
            {
                Element.CurrentPage = Element.Children[selectedIndex];
            }

            SetSelectedTabIcon(tab, child.Icon, true);
        }

        void TabLayout.IOnTabSelectedListener.OnTabUnselected(TabLayout.Tab tab)
        {
            var child = Element.Children[tab.Position];
            SetSelectedTabIcon(tab, child.Icon, false);
        }

        private void SetSelectedTabIcon(TabLayout.Tab tab, string icon, bool selected)
        {
            if(string.IsNullOrEmpty(icon))
            {
                return;
            }

            if(selected)
            {
                var selectedResource = IdFromTitle(string.Format("{0}_selected", icon), ResourceManager.DrawableClass);
                if(selectedResource != 0)
                {
                    tab.SetIcon(selectedResource);
                    return;
                }
            }

            var resource = IdFromTitle(icon, ResourceManager.DrawableClass);
            tab.SetIcon(resource);
        }

        private int IdFromTitle(string title, Type type)
        {
            var name = System.IO.Path.GetFileNameWithoutExtension(title);
            return GetId(type, name);
        }

        private int GetId(Type type, string propertyName)
        {
            var props = type.GetFields();
            var prop = props.FirstOrDefault(p => p.Name == propertyName);
            if(prop != null)
            {
                return (int)prop.GetValue(type);
            }

            return 0;
        }
    }
}
