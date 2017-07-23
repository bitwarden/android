using Bit.App.Models.Page;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class VaultListViewCell : LabeledDetailCell
    {
        public static readonly BindableProperty LoginParameterProperty = BindableProperty.Create(nameof(LoginParameter),
            typeof(VaultListPageModel.Login), typeof(VaultListViewCell), null);

        public VaultListViewCell(Action<VaultListPageModel.Login> moreClickedAction)
        {
            SetBinding(LoginParameterProperty, new Binding("."));
            Label.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Login.Name));
            Detail.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Login.Username));
            LabelIcon.SetBinding(VisualElement.IsVisibleProperty, nameof(VaultListPageModel.Login.Shared));
            LabelIcon2.SetBinding(VisualElement.IsVisibleProperty, nameof(VaultListPageModel.Login.HasAttachments));

            Button.Image = "more";
            Button.Command = new Command(() => moreClickedAction?.Invoke(LoginParameter));
            Button.BackgroundColor = Color.Transparent;

            LabelIcon.Source = "share";
            LabelIcon2.Source = "paperclip";

            BackgroundColor = Color.White;
        }

        public VaultListPageModel.Login LoginParameter
        {
            get { return GetValue(LoginParameterProperty) as VaultListPageModel.Login; }
            set { SetValue(LoginParameterProperty, value); }
        }
    }
}
