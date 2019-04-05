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
        public static new BindableProperty BackgroundColorProperty = BindableProperty.Create(
            nameof(BackgroundColor), typeof(Color), typeof(BoxedView), default(Color),
                defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty SeparatorColorProperty = BindableProperty.Create(
            nameof(SeparatorColor), typeof(Color), typeof(BoxedView), Color.FromRgb(199, 199, 204),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty SelectedColorProperty = BindableProperty.Create(
            nameof(SelectedColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty HeaderPaddingProperty = BindableProperty.Create(
            nameof(HeaderPadding), typeof(Thickness), typeof(BoxedView), new Thickness(14, 8, 8, 8),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty HeaderTextColorProperty = BindableProperty.Create(
            nameof(HeaderTextColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty HeaderFontSizeProperty = BindableProperty.Create(
            nameof(HeaderFontSize), typeof(double), typeof(BoxedView), -1.0d,
            defaultBindingMode: BindingMode.OneWay,
            defaultValueCreator: bindable => Device.GetNamedSize(NamedSize.Small, (BoxedView)bindable));

        public static BindableProperty HeaderTextVerticalAlignProperty = BindableProperty.Create(
            nameof(HeaderTextVerticalAlign), typeof(LayoutAlignment), typeof(BoxedView), LayoutAlignment.End,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty HeaderBackgroundColorProperty = BindableProperty.Create(
            nameof(HeaderBackgroundColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty HeaderHeightProperty = BindableProperty.Create(
            nameof(HeaderHeight), typeof(double), typeof(BoxedView), -1d, defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty FooterTextColorProperty = BindableProperty.Create(
            nameof(FooterTextColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty FooterFontSizeProperty = BindableProperty.Create(
            nameof(FooterFontSize), typeof(double), typeof(BoxedView), -1.0d,
            defaultBindingMode: BindingMode.OneWay,
            defaultValueCreator: bindable => Device.GetNamedSize(NamedSize.Small, (BoxedView)bindable));

        public static BindableProperty FooterBackgroundColorProperty = BindableProperty.Create(
            nameof(FooterBackgroundColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty FooterPaddingProperty = BindableProperty.Create(
            nameof(FooterPadding), typeof(Thickness), typeof(BoxedView), new Thickness(14, 8, 14, 8),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty CellTitleColorProperty = BindableProperty.Create(
            nameof(CellTitleColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty CellTitleFontSizeProperty = BindableProperty.Create(
            nameof(CellTitleFontSize), typeof(double), typeof(BoxedView), -1.0,
            defaultBindingMode: BindingMode.OneWay,
            defaultValueCreator: bindable => Device.GetNamedSize(NamedSize.Default, (BoxedView)bindable));

        public static BindableProperty CellValueTextColorProperty = BindableProperty.Create(
            nameof(CellValueTextColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty CellValueTextFontSizeProperty = BindableProperty.Create(
            nameof(CellValueTextFontSize), typeof(double), typeof(BoxedView), -1.0d,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty CellBackgroundColorProperty = BindableProperty.Create(
            nameof(CellBackgroundColor), typeof(Color), typeof(BoxedView), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty CellAccentColorProperty = BindableProperty.Create(
            nameof(CellAccentColor), typeof(Color), typeof(BoxedView), Color.Accent,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ShowSectionTopBottomBorderProperty = BindableProperty.Create(
            nameof(ShowSectionTopBottomBorder), typeof(bool), typeof(BoxedView), true,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ScrollToBottomProperty = BindableProperty.Create(
            nameof(ScrollToBottom), typeof(bool), typeof(BoxedView), default(bool),
            defaultBindingMode: BindingMode.TwoWay);

        public static BindableProperty ScrollToTopProperty = BindableProperty.Create(
            nameof(ScrollToTop), typeof(bool), typeof(BoxedView), default(bool),
            defaultBindingMode: BindingMode.TwoWay);

        public static BindableProperty VisibleContentHeightProperty = BindableProperty.Create(
            nameof(VisibleContentHeight), typeof(double), typeof(BoxedView), -1d,
            defaultBindingMode: BindingMode.OneWayToSource);

        public static BindableProperty ItemsSourceProperty = BindableProperty.Create(
            nameof(ItemsSource), typeof(IEnumerable), typeof(BoxedView), default(IEnumerable),
            defaultBindingMode: BindingMode.OneWay, propertyChanged: ItemsChanged);

        public static BindableProperty ItemTemplateProperty = BindableProperty.Create(
            nameof(ItemTemplate), typeof(DataTemplate), typeof(BoxedView), default(DataTemplate),
            defaultBindingMode: BindingMode.OneWay);

        private BoxedRoot _root;

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

        // Make the unnecessary property existing at TableView sealed.
        private new int Intent { get; set; }

        public new Color BackgroundColor
        {
            get => (Color)GetValue(BackgroundColorProperty);
            set => SetValue(BackgroundColorProperty, value);
        }

        public Color SeparatorColor
        {
            get => (Color)GetValue(SeparatorColorProperty);
            set => SetValue(SeparatorColorProperty, value);
        }

        public Color SelectedColor
        {
            get => (Color)GetValue(SelectedColorProperty);
            set => SetValue(SelectedColorProperty, value);
        }

        public Thickness HeaderPadding
        {
            get => (Thickness)GetValue(HeaderPaddingProperty);
            set => SetValue(HeaderPaddingProperty, value);
        }

        public Color HeaderTextColor
        {
            get => (Color)GetValue(HeaderTextColorProperty);
            set => SetValue(HeaderTextColorProperty, value);
        }

        [TypeConverter(typeof(FontSizeConverter))]
        public double HeaderFontSize
        {
            get => (double)GetValue(HeaderFontSizeProperty);
            set => SetValue(HeaderFontSizeProperty, value);
        }

        public LayoutAlignment HeaderTextVerticalAlign
        {
            get => (LayoutAlignment)GetValue(HeaderTextVerticalAlignProperty);
            set => SetValue(HeaderTextVerticalAlignProperty, value);
        }

        public Color HeaderBackgroundColor
        {
            get => (Color)GetValue(HeaderBackgroundColorProperty);
            set => SetValue(HeaderBackgroundColorProperty, value);
        }

        public double HeaderHeight
        {
            get => (double)GetValue(HeaderHeightProperty);
            set => SetValue(HeaderHeightProperty, value);
        }

        public Color FooterTextColor
        {
            get => (Color)GetValue(FooterTextColorProperty);
            set => SetValue(FooterTextColorProperty, value);
        }

        [TypeConverter(typeof(FontSizeConverter))]
        public double FooterFontSize
        {
            get => (double)GetValue(FooterFontSizeProperty);
            set => SetValue(FooterFontSizeProperty, value);
        }

        public Color FooterBackgroundColor
        {
            get => (Color)GetValue(FooterBackgroundColorProperty);
            set => SetValue(FooterBackgroundColorProperty, value);
        }

        public Thickness FooterPadding
        {
            get => (Thickness)GetValue(FooterPaddingProperty);
            set => SetValue(FooterPaddingProperty, value);
        }

        public Color CellTitleColor
        {
            get => (Color)GetValue(CellTitleColorProperty);
            set => SetValue(CellTitleColorProperty, value);
        }

        [TypeConverter(typeof(FontSizeConverter))]
        public double CellTitleFontSize
        {
            get => (double)GetValue(CellTitleFontSizeProperty);
            set => SetValue(CellTitleFontSizeProperty, value);
        }

        public Color CellValueTextColor
        {
            get => (Color)GetValue(CellValueTextColorProperty);
            set => SetValue(CellValueTextColorProperty, value);
        }

        [TypeConverter(typeof(FontSizeConverter))]
        public double CellValueTextFontSize
        {
            get => (double)GetValue(CellValueTextFontSizeProperty);
            set => SetValue(CellValueTextFontSizeProperty, value);
        }

        public Color CellBackgroundColor
        {
            get => (Color)GetValue(CellBackgroundColorProperty);
            set => SetValue(CellBackgroundColorProperty, value);
        }

        public Color CellAccentColor
        {
            get => (Color)GetValue(CellAccentColorProperty);
            set => SetValue(CellAccentColorProperty, value);
        }

        public bool ShowSectionTopBottomBorder
        {
            get => (bool)GetValue(ShowSectionTopBottomBorderProperty);
            set => SetValue(ShowSectionTopBottomBorderProperty, value);
        }

        public bool ScrollToBottom
        {
            get => (bool)GetValue(ScrollToBottomProperty);
            set => SetValue(ScrollToBottomProperty, value);
        }

        public bool ScrollToTop
        {
            get => (bool)GetValue(ScrollToTopProperty);
            set => SetValue(ScrollToTopProperty, value);
        }

        public double VisibleContentHeight
        {
            get => (double)GetValue(VisibleContentHeightProperty);
            set => SetValue(VisibleContentHeightProperty, value);
        }

        public IEnumerable ItemsSource
        {
            get => (IEnumerable)GetValue(ItemsSourceProperty);
            set => SetValue(ItemsSourceProperty, value);
        }

        public DataTemplate ItemTemplate
        {
            get => (DataTemplate)GetValue(ItemTemplateProperty);
            set => SetValue(ItemTemplateProperty, value);
        }

        public new event EventHandler ModelChanged;

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
            var changed = e.PropertyName == TableSectionBase.TitleProperty.PropertyName ||
                e.PropertyName == BoxedSection.FooterTextProperty.PropertyName ||
                e.PropertyName == BoxedSection.IsVisibleProperty.PropertyName;
            if(changed)
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
