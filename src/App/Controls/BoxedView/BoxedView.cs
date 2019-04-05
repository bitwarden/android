using System;
using System.Collections;
using System.Collections.Specialized;
using System.Linq;
using Xamarin.Forms;

namespace Bit.App.Controls.BoxedView
{
    [ContentProperty("Root")]
    public class BoxedView : TableView
    {
        private BoxedRoot _root;

        public new event EventHandler ModelChanged;

        public BoxedView()
        {
            VerticalOptions = HorizontalOptions = LayoutOptions.FillAndExpand;
            Root = new BoxedRoot();
            Model = new BoxedModel(Root);
        }

        public new BoxedModel Model { get; set; }

        public new BoxedRoot Root
        {
            get => _root;
            set
            {
                if(_root != null)
                {
                    _root.PropertyChanged -= RootOnPropertyChanged;
                    _root.CollectionChanged -= OnCollectionChanged;
                    _root.SectionCollectionChanged -= OnSectionCollectionChanged;
                }

                _root = value;

                // Transfer binding context to the children (maybe...)
                SetInheritedBindingContext(_root, BindingContext);

                _root.PropertyChanged += RootOnPropertyChanged;
                _root.CollectionChanged += OnCollectionChanged;
                _root.SectionCollectionChanged += OnSectionCollectionChanged;
            }
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();
            if(Root != null)
            {
                SetInheritedBindingContext(Root, BindingContext);
            }
        }

        private void RootOnPropertyChanged(object sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if(e.PropertyName == TableSectionBase.TitleProperty.PropertyName ||
                e.PropertyName == BoxedSection.FooterTextProperty.PropertyName ||
                e.PropertyName == BoxedSection.IsVisibleProperty.PropertyName)
            {
                OnModelChanged();
            }
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            var changed = propertyName == HasUnevenRowsProperty.PropertyName ||
                propertyName == HeaderHeightProperty.PropertyName ||
                propertyName == HeaderFontSizeProperty.PropertyName ||
                propertyName == HeaderTextColorProperty.PropertyName ||
                propertyName == HeaderBackgroundColorProperty.PropertyName ||
                propertyName == HeaderTextVerticalAlignProperty.PropertyName ||
                propertyName == HeaderPaddingProperty.PropertyName ||
                propertyName == FooterFontSizeProperty.PropertyName ||
                propertyName == FooterTextColorProperty.PropertyName ||
                propertyName == FooterBackgroundColorProperty.PropertyName ||
                propertyName == FooterPaddingProperty.PropertyName;
            if(changed)
            {
                OnModelChanged();
            }
        }

        public void OnCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            OnModelChanged();
        }

        public void OnSectionCollectionChanged(object sender, EventArgs childCollectionChangedEventArgs)
        {
            OnModelChanged();
        }

        protected new void OnModelChanged()
        {
            var cells = Root?.SelectMany(r => r);
            if(cells == null)
            {
                return;
            }
            foreach(var cell in cells)
            {
                cell.Parent = this;
            }
            ModelChanged?.Invoke(this, EventArgs.Empty);
        }

        // Make the unnecessary property existing at TableView sealed.
        private new int Intent { get; set; }

        public static new BindableProperty BackgroundColorProperty =
            BindableProperty.Create(
                nameof(BackgroundColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public new Color BackgroundColor
        {
            get { return (Color)GetValue(BackgroundColorProperty); }
            set { SetValue(BackgroundColorProperty, value); }
        }

        public static BindableProperty SeparatorColorProperty =
            BindableProperty.Create(
                nameof(SeparatorColor),
                typeof(Color),
                typeof(BoxedView),
                Color.FromRgb(199, 199, 204),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color SeparatorColor
        {
            get { return (Color)GetValue(SeparatorColorProperty); }
            set { SetValue(SeparatorColorProperty, value); }
        }

        public static BindableProperty SelectedColorProperty =
            BindableProperty.Create(
                nameof(SelectedColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color SelectedColor
        {
            get { return (Color)GetValue(SelectedColorProperty); }
            set { SetValue(SelectedColorProperty, value); }
        }

        public static BindableProperty HeaderPaddingProperty =
            BindableProperty.Create(
                nameof(HeaderPadding),
                typeof(Thickness),
                typeof(BoxedView),
                new Thickness(14, 8, 8, 8),
                defaultBindingMode: BindingMode.OneWay
            );

        public Thickness HeaderPadding
        {
            get { return (Thickness)GetValue(HeaderPaddingProperty); }
            set { SetValue(HeaderPaddingProperty, value); }
        }

        public static BindableProperty HeaderTextColorProperty =
            BindableProperty.Create(
                nameof(HeaderTextColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color HeaderTextColor
        {
            get { return (Color)GetValue(HeaderTextColorProperty); }
            set { SetValue(HeaderTextColorProperty, value); }
        }

        public static BindableProperty HeaderFontSizeProperty =
            BindableProperty.Create(
                nameof(HeaderFontSize),
                typeof(double),
                typeof(BoxedView),
                -1.0d,
                defaultBindingMode: BindingMode.OneWay,
                defaultValueCreator: bindable => Device.GetNamedSize(NamedSize.Small, (BoxedView)bindable)
            );

        [TypeConverter(typeof(FontSizeConverter))]
        public double HeaderFontSize
        {
            get { return (double)GetValue(HeaderFontSizeProperty); }
            set { SetValue(HeaderFontSizeProperty, value); }
        }

        public static BindableProperty HeaderTextVerticalAlignProperty =
            BindableProperty.Create(
                nameof(HeaderTextVerticalAlign),
                typeof(LayoutAlignment),
                typeof(BoxedView),
                LayoutAlignment.End,
                defaultBindingMode: BindingMode.OneWay
            );

        public LayoutAlignment HeaderTextVerticalAlign
        {
            get { return (LayoutAlignment)GetValue(HeaderTextVerticalAlignProperty); }
            set { SetValue(HeaderTextVerticalAlignProperty, value); }
        }

        public static BindableProperty HeaderBackgroundColorProperty =
            BindableProperty.Create(
                nameof(HeaderBackgroundColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color HeaderBackgroundColor
        {
            get { return (Color)GetValue(HeaderBackgroundColorProperty); }
            set { SetValue(HeaderBackgroundColorProperty, value); }
        }

        public static BindableProperty HeaderHeightProperty =
            BindableProperty.Create(
                nameof(HeaderHeight),
                typeof(double),
                typeof(BoxedView),
                -1d,
                defaultBindingMode: BindingMode.OneWay
            );

        public double HeaderHeight
        {
            get { return (double)GetValue(HeaderHeightProperty); }
            set { SetValue(HeaderHeightProperty, value); }
        }

        public static BindableProperty FooterTextColorProperty =
            BindableProperty.Create(
                nameof(FooterTextColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color FooterTextColor
        {
            get { return (Color)GetValue(FooterTextColorProperty); }
            set { SetValue(FooterTextColorProperty, value); }
        }

        public static BindableProperty FooterFontSizeProperty =
            BindableProperty.Create(
                nameof(FooterFontSize),
                typeof(double),
                typeof(BoxedView),
                -1.0d,
                defaultBindingMode: BindingMode.OneWay,
                defaultValueCreator: bindable => Device.GetNamedSize(NamedSize.Small, (BoxedView)bindable)
            );

        [TypeConverter(typeof(FontSizeConverter))]
        public double FooterFontSize
        {
            get { return (double)GetValue(FooterFontSizeProperty); }
            set { SetValue(FooterFontSizeProperty, value); }
        }

        public static BindableProperty FooterBackgroundColorProperty =
            BindableProperty.Create(
                nameof(FooterBackgroundColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color FooterBackgroundColor
        {
            get { return (Color)GetValue(FooterBackgroundColorProperty); }
            set { SetValue(FooterBackgroundColorProperty, value); }
        }

        public static BindableProperty FooterPaddingProperty =
            BindableProperty.Create(
                nameof(FooterPadding),
                typeof(Thickness),
                typeof(BoxedView),
                new Thickness(14, 8, 14, 8),
                defaultBindingMode: BindingMode.OneWay
            );

        public Thickness FooterPadding
        {
            get { return (Thickness)GetValue(FooterPaddingProperty); }
            set { SetValue(FooterPaddingProperty, value); }
        }

        public static BindableProperty CellTitleColorProperty =
            BindableProperty.Create(
                nameof(CellTitleColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color CellTitleColor
        {
            get { return (Color)GetValue(CellTitleColorProperty); }
            set { SetValue(CellTitleColorProperty, value); }
        }

        public static BindableProperty CellTitleFontSizeProperty =
            BindableProperty.Create(
                nameof(CellTitleFontSize),
                typeof(double),
                typeof(BoxedView),
                -1.0,
                defaultBindingMode: BindingMode.OneWay,
                defaultValueCreator: bindable => Device.GetNamedSize(NamedSize.Default, (BoxedView)bindable)
            );


        [TypeConverter(typeof(FontSizeConverter))]
        public double CellTitleFontSize
        {
            get { return (double)GetValue(CellTitleFontSizeProperty); }
            set { SetValue(CellTitleFontSizeProperty, value); }
        }

        public static BindableProperty CellValueTextColorProperty =
            BindableProperty.Create(
                nameof(CellValueTextColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color CellValueTextColor
        {
            get { return (Color)GetValue(CellValueTextColorProperty); }
            set { SetValue(CellValueTextColorProperty, value); }
        }

        public static BindableProperty CellValueTextFontSizeProperty =
            BindableProperty.Create(
                nameof(CellValueTextFontSize),
                typeof(double),
                typeof(BoxedView),
                -1.0d,
                defaultBindingMode: BindingMode.OneWay
            );

        [TypeConverter(typeof(FontSizeConverter))]
        public double CellValueTextFontSize
        {
            get { return (double)GetValue(CellValueTextFontSizeProperty); }
            set { SetValue(CellValueTextFontSizeProperty, value); }
        }

        public static BindableProperty CellDescriptionColorProperty =
            BindableProperty.Create(
                nameof(CellDescriptionColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color CellDescriptionColor
        {
            get { return (Color)GetValue(CellDescriptionColorProperty); }
            set { SetValue(CellDescriptionColorProperty, value); }
        }

        public static BindableProperty CellDescriptionFontSizeProperty =
            BindableProperty.Create(
                nameof(CellDescriptionFontSize),
                typeof(double),
                typeof(BoxedView),
                -1.0d,
                defaultBindingMode: BindingMode.OneWay
            );

        [TypeConverter(typeof(FontSizeConverter))]
        public double CellDescriptionFontSize
        {
            get { return (double)GetValue(CellDescriptionFontSizeProperty); }
            set { SetValue(CellDescriptionFontSizeProperty, value); }
        }

        public static BindableProperty CellBackgroundColorProperty =
            BindableProperty.Create(
                nameof(CellBackgroundColor),
                typeof(Color),
                typeof(BoxedView),
                default(Color),
                defaultBindingMode: BindingMode.OneWay
            );

        public Color CellBackgroundColor
        {
            get { return (Color)GetValue(CellBackgroundColorProperty); }
            set { SetValue(CellBackgroundColorProperty, value); }
        }

        public static BindableProperty CellAccentColorProperty =
            BindableProperty.Create(
                nameof(CellAccentColor),
                typeof(Color),
                typeof(BoxedView),
                Color.Accent,
                defaultBindingMode: BindingMode.OneWay
            );

        public Color CellAccentColor
        {
            get { return (Color)GetValue(CellAccentColorProperty); }
            set { SetValue(CellAccentColorProperty, value); }
        }

        public static BindableProperty ShowSectionTopBottomBorderProperty =
            BindableProperty.Create(
                nameof(ShowSectionTopBottomBorder),
                typeof(bool),
                typeof(BoxedView),
                true,
                defaultBindingMode: BindingMode.OneWay
            );

        public bool ShowSectionTopBottomBorder
        {
            get { return (bool)GetValue(ShowSectionTopBottomBorderProperty); }
            set { SetValue(ShowSectionTopBottomBorderProperty, value); }
        }

        public static BindableProperty ScrollToBottomProperty =
            BindableProperty.Create(
                nameof(ScrollToBottom),
                typeof(bool),
                typeof(BoxedView),
                default(bool),
                defaultBindingMode: BindingMode.TwoWay
            );

        public bool ScrollToBottom
        {
            get { return (bool)GetValue(ScrollToBottomProperty); }
            set { SetValue(ScrollToBottomProperty, value); }
        }

        public static BindableProperty ScrollToTopProperty =
            BindableProperty.Create(
                nameof(ScrollToTop),
                typeof(bool),
                typeof(BoxedView),
                default(bool),
                defaultBindingMode: BindingMode.TwoWay
            );

        public bool ScrollToTop
        {
            get { return (bool)GetValue(ScrollToTopProperty); }
            set { SetValue(ScrollToTopProperty, value); }
        }

        public static BindableProperty VisibleContentHeightProperty =
            BindableProperty.Create(
                nameof(VisibleContentHeight),
                typeof(double),
                typeof(BoxedView),
                -1d,
                defaultBindingMode: BindingMode.OneWayToSource
            );

        public double VisibleContentHeight
        {
            get { return (double)GetValue(VisibleContentHeightProperty); }
            set { SetValue(VisibleContentHeightProperty, value); }
        }

        public static BindableProperty ItemsSourceProperty =
            BindableProperty.Create(
                nameof(ItemsSource),
                typeof(IEnumerable),
                typeof(BoxedView),
                default(IEnumerable),
                defaultBindingMode: BindingMode.OneWay,
                propertyChanged: ItemsChanged
            );

        public IEnumerable ItemsSource
        {
            get { return (IEnumerable)GetValue(ItemsSourceProperty); }
            set { SetValue(ItemsSourceProperty, value); }
        }

        public static BindableProperty ItemTemplateProperty =
            BindableProperty.Create(
                nameof(ItemTemplate),
                typeof(DataTemplate),
                typeof(BoxedView),
                default(DataTemplate),
                defaultBindingMode: BindingMode.OneWay
            );

        public DataTemplate ItemTemplate
        {
            get { return (DataTemplate)GetValue(ItemTemplateProperty); }
            set { SetValue(ItemTemplateProperty, value); }
        }

        private static void ItemsChanged(BindableObject bindable, object oldValue, object newValue)
        {
            var boxedView = bindable as BoxedView;
            if(boxedView.ItemTemplate == null)
            {
                return;
            }

            if(oldValue is INotifyCollectionChanged oldObservableCollection)
            {
                oldObservableCollection.CollectionChanged -= boxedView.OnItemsSourceCollectionChanged;
            }
            if(newValue is INotifyCollectionChanged newObservableCollection)
            {
                newObservableCollection.CollectionChanged += boxedView.OnItemsSourceCollectionChanged;
            }

            boxedView.Root.Clear();

            if(newValue is IList newValueAsEnumerable)
            {
                foreach(var item in newValueAsEnumerable)
                {
                    var view = CreateChildViewFor(boxedView.ItemTemplate, item, boxedView);
                    boxedView.Root.Add(view);
                }
            }
        }

        private void OnItemsSourceCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            if(e.Action == NotifyCollectionChangedAction.Replace)
            {
                Root.RemoveAt(e.OldStartingIndex);
                var item = e.NewItems[e.NewStartingIndex];
                var view = CreateChildViewFor(ItemTemplate, item, this);
                Root.Insert(e.NewStartingIndex, view);
            }
            else if(e.Action == NotifyCollectionChangedAction.Add)
            {
                if(e.NewItems != null)
                {
                    for(var i = 0; i < e.NewItems.Count; ++i)
                    {
                        var item = e.NewItems[i];
                        var view = CreateChildViewFor(ItemTemplate, item, this);
                        Root.Insert(i + e.NewStartingIndex, view);
                    }
                }
            }
            else if(e.Action == NotifyCollectionChangedAction.Remove)
            {
                if(e.OldItems != null)
                {
                    Root.RemoveAt(e.OldStartingIndex);
                }
            }
            else if(e.Action == NotifyCollectionChangedAction.Reset)
            {
                Root.Clear();
            }
        }

        private static BoxedSection CreateChildViewFor(DataTemplate template, object item, BindableObject container)
        {
            if(template is DataTemplateSelector selector)
            {
                template = selector.SelectTemplate(item, container);
            }
            template.SetValue(BindingContextProperty, item);
            return template.CreateContent() as BoxedSection;
        }
    }
}
