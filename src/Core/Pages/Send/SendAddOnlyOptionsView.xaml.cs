using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using Bit.App.Behaviors;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using CommunityToolkit.Maui.Converters;
using CommunityToolkit.Maui.ImageSources;
using CommunityToolkit.Maui;
using CommunityToolkit.Maui.Core;
using CommunityToolkit.Maui.Layouts;
using CommunityToolkit.Maui.Views;

namespace Bit.App.Pages
{
    public partial class SendAddOnlyOptionsView : ContentView
    {
        public SendAddOnlyOptionsView()
        {
            InitializeComponent();
        }

        private SendAddEditPageViewModel ViewModel => BindingContext as SendAddEditPageViewModel;

        public void SetMainScrollView(ScrollView scrollView)
        {
            _notesEditor.Behaviors.Add(new EditorPreventAutoBottomScrollingOnFocusedBehavior { ParentScrollView = scrollView });
        }

        private void OnMaxAccessCountTextChanged(object sender, TextChangedEventArgs e)
        {
            if (ViewModel is null)
            {
                return;
            }

            if (string.IsNullOrWhiteSpace(e.NewTextValue))
            {
                ViewModel.MaxAccessCount = null;
                _maxAccessCountStepper.Value = 0;
                return;
            }
            // accept only digits
            if (!int.TryParse(e.NewTextValue, out int _))
            {
                ((Entry)sender).Text = e.OldTextValue;
            }
        }

        protected override void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (propertyName == nameof(BindingContext)
               &&
               ViewModel != null)
            {
                ViewModel.PropertyChanged += ViewModel_PropertyChanged;
            }
        }

        private void ViewModel_PropertyChanged(object sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if (!_lazyDeletionDateTimePicker.IsLoaded
                &&
                e.PropertyName == nameof(SendAddEditPageViewModel.ShowDeletionCustomPickers)
                &&
                ViewModel.ShowDeletionCustomPickers)
            {
                _lazyDeletionDateTimePicker.LoadViewAsync();
            }

            if (!_lazyExpirationDateTimePicker.IsLoaded
                &&
                e.PropertyName == nameof(SendAddEditPageViewModel.ShowExpirationCustomPickers)
                &&
                ViewModel.ShowExpirationCustomPickers)
            {
                _lazyExpirationDateTimePicker.LoadViewAsync();
            }
        }
    }

    public class SendAddOnlyOptionsLazyView : LazyView<SendAddOnlyOptionsView>
    {
        public ScrollView MainScrollView { get; set; }

        public override async ValueTask LoadViewAsync()
        {
            await base.LoadViewAsync();

            if (Content is SendAddOnlyOptionsView optionsView)
            {
                optionsView.SetMainScrollView(MainScrollView);
            }
        }
    }
}
