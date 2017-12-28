using System;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Com.Ittianyu.Bottomnavigationviewex;
using Xamarin.Forms.Platform.Android;
using RelativeLayout = Android.Widget.RelativeLayout;
using Platform = Xamarin.Forms.Platform.Android.Platform;
using Android.Content;
using Android.Views;
using Android.Widget;
using Android.Support.Design.Internal;
using System.IO;
using System.Linq;
using System.ComponentModel;
using Android.Support.Design.Widget;

[assembly: ExportRenderer(typeof(ExtendedTabbedPage), typeof(ExtendedTabbedPageRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedTabbedPageRenderer : VisualElementRenderer<ExtendedTabbedPage>,
        BottomNavigationView.IOnNavigationItemSelectedListener
    {
        public static bool ShouldUpdateSelectedIcon;
        public static Action<IMenuItem, FileImageSource, bool> MenuItemIconSetter;
        public static float? BottomBarHeight = 50;

        private RelativeLayout _rootLayout;
        private FrameLayout _pageContainer;
        private BottomNavigationViewEx _bottomNav;
        private readonly int _barId;

        public static global::Android.Graphics.Color? BackgroundColor;

        public ExtendedTabbedPageRenderer(Context context)
            : base(context)
        {
            AutoPackage = false;
            _barId = GenerateViewId();
        }

        IPageController TabbedController => Element as IPageController;
        public int LastSelectedIndex { get; internal set; }

        public bool OnNavigationItemSelected(IMenuItem item)
        {
            this.SwitchPage(item);
            return true;
        }

        internal void SetupTabItems()
        {
            this.SetupTabItems(_bottomNav);
        }

        internal void SetupBottomBar()
        {
            _bottomNav = this.SetupBottomBar(_rootLayout, _bottomNav, _barId);
        }

        public static readonly Action<IMenuItem, FileImageSource, bool> DefaultMenuItemIconSetter = (menuItem, icon, selected) =>
        {
            var tabIconId = ResourceUtils.IdFromTitle(icon, ResourceManager.DrawableClass);
            menuItem.SetIcon(tabIconId);
        };

        protected override void OnElementChanged(ElementChangedEventArgs<ExtendedTabbedPage> e)
        {
            base.OnElementChanged(e);

            if(e.OldElement != null)
            {
                e.OldElement.ChildAdded -= PagesChanged;
                e.OldElement.ChildRemoved -= PagesChanged;
                e.OldElement.ChildrenReordered -= PagesChanged;
            }

            if(e.NewElement == null)
            {
                return;
            }

            UpdateIgnoreContainerAreas();

            if(_rootLayout == null)
            {
                SetupNativeView();
            }

            this.HandlePagesChanged();
            SwitchContent(Element.CurrentPage);

            Element.ChildAdded += PagesChanged;
            Element.ChildRemoved += PagesChanged;
            Element.ChildrenReordered += PagesChanged;
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if(e.PropertyName == nameof(TabbedPage.CurrentPage))
            {
                SwitchContent(Element.CurrentPage);
            }
        }

        void PagesChanged(object sender, EventArgs e)
        {
            this.HandlePagesChanged();
        }

        protected override void OnAttachedToWindow()
        {
            base.OnAttachedToWindow();
            TabbedController?.SendAppearing();
        }

        protected override void OnDetachedFromWindow()
        {
            base.OnDetachedFromWindow();
            TabbedController?.SendDisappearing();
        }

        protected override void OnLayout(bool changed, int left, int top, int right, int bottom)
        {
            var width = right - left;
            var height = bottom - top;

            base.OnLayout(changed, left, top, right, bottom);
            if(width <= 0 || height <= 0)
            {
                return;
            }

            this.Layout(width, height);
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                Element.ChildAdded -= PagesChanged;
                Element.ChildRemoved -= PagesChanged;
                Element.ChildrenReordered -= PagesChanged;

                if(_rootLayout != null)
                {
                    RemoveAllViews();
                    foreach(Page pageToRemove in Element.Children)
                    {
                        var pageRenderer = Platform.GetRenderer(pageToRemove);
                        if(pageRenderer != null)
                        {
                            pageRenderer.View.RemoveFromParent();
                            pageRenderer.Dispose();
                        }
                    }

                    if(_bottomNav != null)
                    {
                        _bottomNav.SetOnNavigationItemSelectedListener(null);
                        _bottomNav.Dispose();
                        _bottomNav = null;
                    }
                    _rootLayout.Dispose();
                    _rootLayout = null;
                }
            }

            base.Dispose(disposing);
        }

        internal void SetupNativeView()
        {
            _rootLayout = this.CreateRoot(_barId, GenerateViewId(), out _pageContainer);
            AddView(_rootLayout);
        }

        void SwitchContent(Page page)
        {
            this.ChangePage(_pageContainer, page);
        }

        void UpdateIgnoreContainerAreas()
        {
            foreach(var child in Element.Children)
            {
                child.IgnoresContainerArea = false;
            }
        }
    }

    internal static class TabExtensions
    {
        public static Rectangle CreateRect(this Context context, int width, int height)
        {
            return new Rectangle(0, 0, context.FromPixels(width), context.FromPixels(height));
        }

        public static void HandlePagesChanged(this ExtendedTabbedPageRenderer renderer)
        {
            renderer.SetupBottomBar();
            renderer.SetupTabItems();

            if(renderer.Element.Children.Count == 0)
            {
                return;
            }

            EnsureTabIndex(renderer);
        }

        static void EnsureTabIndex(ExtendedTabbedPageRenderer renderer)
        {
            var rootLayout = (RelativeLayout)renderer.GetChildAt(0);
            var bottomNav = (BottomNavigationViewEx)rootLayout.GetChildAt(1);
            var menu = (BottomNavigationMenu)bottomNav.Menu;

            var itemIndex = menu.FindItemIndex(bottomNav.SelectedItemId);
            var pageIndex = renderer.Element.Children.IndexOf(renderer.Element.CurrentPage);
            if(pageIndex >= 0 && pageIndex != itemIndex && pageIndex < bottomNav.ItemCount)
            {
                var menuItem = menu.GetItem(pageIndex);
                bottomNav.SelectedItemId = menuItem.ItemId;

                if(ExtendedTabbedPageRenderer.ShouldUpdateSelectedIcon && ExtendedTabbedPageRenderer.MenuItemIconSetter != null)
                {
                    ExtendedTabbedPageRenderer.MenuItemIconSetter?.Invoke(menuItem, renderer.Element.CurrentPage.Icon, true);

                    if(renderer.LastSelectedIndex != pageIndex)
                    {
                        var lastSelectedPage = renderer.Element.Children[renderer.LastSelectedIndex];
                        var lastSelectedMenuItem = menu.GetItem(renderer.LastSelectedIndex);
                        ExtendedTabbedPageRenderer.MenuItemIconSetter?.Invoke(lastSelectedMenuItem, lastSelectedPage.Icon, false);
                        renderer.LastSelectedIndex = pageIndex;
                    }
                }
            }
        }

        public static void SwitchPage(this ExtendedTabbedPageRenderer renderer, IMenuItem item)
        {
            var rootLayout = (RelativeLayout)renderer.GetChildAt(0);
            var bottomNav = (BottomNavigationViewEx)rootLayout.GetChildAt(1);
            var menu = (BottomNavigationMenu)bottomNav.Menu;

            var index = menu.FindItemIndex(item.ItemId);
            var pageIndex = index % renderer.Element.Children.Count;
            var currentPageIndex = renderer.Element.Children.IndexOf(renderer.Element.CurrentPage);

            if(currentPageIndex != pageIndex)
            {
                renderer.Element.CurrentPage = renderer.Element.Children[pageIndex];
            }
        }

        public static void Layout(this ExtendedTabbedPageRenderer renderer, int width, int height)
        {
            var rootLayout = (RelativeLayout)renderer.GetChildAt(0);
            var bottomNav = (BottomNavigationViewEx)rootLayout.GetChildAt(1);

            var Context = renderer.Context;

            rootLayout.Measure(MakeMeasureSpec(width, MeasureSpecMode.Exactly),
                MakeMeasureSpec(height, MeasureSpecMode.AtMost));

            ((IPageController)renderer.Element).ContainerArea =
                Context.CreateRect(rootLayout.MeasuredWidth, rootLayout.GetChildAt(0).MeasuredHeight);

            rootLayout.Measure(MakeMeasureSpec(width, MeasureSpecMode.Exactly),
                MakeMeasureSpec(height, MeasureSpecMode.Exactly));
            rootLayout.Layout(0, 0, rootLayout.MeasuredWidth, rootLayout.MeasuredHeight);

            if(renderer.Element.Children.Count == 0)
            {
                return;
            }

            int tabsHeight = bottomNav.MeasuredHeight;

            var item = (ViewGroup)bottomNav.GetChildAt(0);
            item.Measure(MakeMeasureSpec(width, MeasureSpecMode.Exactly),
                MakeMeasureSpec(tabsHeight, MeasureSpecMode.Exactly));

            item.Layout(0, 0, width, tabsHeight);
            int item_w = width / item.ChildCount;
            for(int i = 0; i < item.ChildCount; i++)
            {
                var frame = (FrameLayout)item.GetChildAt(i);
                frame.Measure(MakeMeasureSpec(item_w, MeasureSpecMode.Exactly),
                    MakeMeasureSpec(tabsHeight, MeasureSpecMode.Exactly));
                frame.Layout(i * item_w, 0, i * item_w + item_w, tabsHeight);
            }
        }

        public static void SetupTabItems(this ExtendedTabbedPageRenderer renderer, BottomNavigationViewEx bottomNav)
        {
            var element = renderer.Element;
            var menu = (BottomNavigationMenu)bottomNav.Menu;
            menu.ClearAll();

            var tabsCount = Math.Min(element.Children.Count, bottomNav.MaxItemCount);
            for(int i = 0; i < tabsCount; i++)
            {
                var page = element.Children[i];
                var menuItem = menu.Add(0, i, 0, page.Title);
                var setter = ExtendedTabbedPageRenderer.MenuItemIconSetter ?? ExtendedTabbedPageRenderer.DefaultMenuItemIconSetter;
                setter.Invoke(menuItem, page.Icon, renderer.LastSelectedIndex == i);
            }

            if(element.Children.Count > 0)
            {
                bottomNav.EnableShiftingMode(false);
                bottomNav.EnableItemShiftingMode(false);
                bottomNav.EnableAnimation(false);
                bottomNav.SetTextVisibility(false);
                bottomNav.SetIconsMarginTop(30);
                bottomNav.SetBackgroundResource(Resource.Drawable.bottom_nav_bg);

                var stateList = new global::Android.Content.Res.ColorStateList(
                    new int[][] {
                        new int[] { global::Android.Resource.Attribute.StateChecked },
                        new int[] { global::Android.Resource.Attribute.StateEnabled}
                    },
                    new int[] {
                        element.TintColor.ToAndroid(), // Selected
                        Color.FromHex("A1A1A1").ToAndroid() // Normal
                    });

                bottomNav.ItemIconTintList = stateList;
            }
        }

        public static BottomNavigationViewEx SetupBottomBar(this ExtendedTabbedPageRenderer renderer,
            global::Android.Widget.RelativeLayout rootLayout, BottomNavigationViewEx bottomNav, int barId)
        {
            if(bottomNav != null)
            {
                rootLayout.RemoveView(bottomNav);
                bottomNav.SetOnNavigationItemSelectedListener(null);
            }

            var barParams = new global::Android.Widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MatchParent,
                ExtendedTabbedPageRenderer.BottomBarHeight.HasValue ?
                (int)rootLayout.Context.ToPixels(ExtendedTabbedPageRenderer.BottomBarHeight.Value) :
                ViewGroup.LayoutParams.WrapContent);
            barParams.AddRule(LayoutRules.AlignParentBottom);
            bottomNav = new BottomNavigationViewEx(rootLayout.Context)
            {
                LayoutParameters = barParams,
                Id = barId
            };
            if(ExtendedTabbedPageRenderer.BackgroundColor.HasValue)
            {
                bottomNav.SetBackgroundColor(ExtendedTabbedPageRenderer.BackgroundColor.Value);
            }

            bottomNav.SetOnNavigationItemSelectedListener(renderer);
            rootLayout.AddView(bottomNav, 1, barParams);

            return bottomNav;
        }

        public static void ChangePage(this ExtendedTabbedPageRenderer renderer, FrameLayout pageContainer, Page page)
        {
            renderer.Context.HideKeyboard(renderer);
            if(page == null)
            {
                return;
            }

            if(Platform.GetRenderer(page) == null)
            {
                Platform.SetRenderer(page, Platform.CreateRendererWithContext(page, renderer.Context));
            }

            var pageContent = Platform.GetRenderer(page).View;
            pageContainer.AddView(pageContent);
            if(pageContainer.ChildCount > 1)
            {
                pageContainer.RemoveViewAt(0);
            }

            EnsureTabIndex(renderer);
        }

        public static RelativeLayout CreateRoot(this ExtendedTabbedPageRenderer renderer, int barId, int pageContainerId, out FrameLayout pageContainer)
        {
            var rootLayout = new RelativeLayout(renderer.Context)
            {
                LayoutParameters = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MatchParent, ViewGroup.LayoutParams.MatchParent),
            };

            var pageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MatchParent, ViewGroup.LayoutParams.MatchParent);
            pageParams.AddRule(LayoutRules.Above, barId);
            pageContainer = new FrameLayout(renderer.Context)
            {
                LayoutParameters = pageParams,
                Id = pageContainerId
            };

            rootLayout.AddView(pageContainer, 0, pageParams);
            return rootLayout;
        }

        private static int MakeMeasureSpec(int size, MeasureSpecMode mode)
        {
            return size + (int)mode;
        }
    }

    public static class ResourceUtils
    {
        public static int IdFromTitle(string title, Type type)
        {
            var name = Path.GetFileNameWithoutExtension(title);
            var id = GetId(type, name);
            return id;
        }

        public static int GetId(Type type, string propertyName)
        {
            var props = type.GetFields();
            var prop = props.Select(p => p).FirstOrDefault(p => p.Name == propertyName);
            if(prop != null)
            {
                return (int)prop.GetValue(type);
            }

            return 0;
        }
    }
}
