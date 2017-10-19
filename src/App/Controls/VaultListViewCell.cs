using Bit.App.Models.Page;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class VaultListViewCell : LabeledDetailCell
    {
        public static readonly BindableProperty LoginParameterProperty = BindableProperty.Create(nameof(LoginParameter),
            typeof(VaultListPageModel.Cipher), typeof(VaultListViewCell), null);

        public VaultListViewCell(Action<VaultListPageModel.Cipher> moreClickedAction)
        {
            SetBinding(LoginParameterProperty, new Binding("."));
            Label.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Cipher.Name));
            Detail.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Cipher.Subtitle));
            LabelIcon.SetBinding(VisualElement.IsVisibleProperty, nameof(VaultListPageModel.Cipher.Shared));
            LabelIcon2.SetBinding(VisualElement.IsVisibleProperty, nameof(VaultListPageModel.Cipher.HasAttachments));

            Button.Image = "more";
            Button.Command = new Command(() => moreClickedAction?.Invoke(LoginParameter));
            Button.BackgroundColor = Color.Transparent;

            LabelIcon.Source = "share";
            LabelIcon2.Source = "paperclip";

            BackgroundColor = Color.White;
        }

        public VaultListPageModel.Cipher LoginParameter
        {
            get { return GetValue(LoginParameterProperty) as VaultListPageModel.Cipher; }
            set { SetValue(LoginParameterProperty, value); }
        }
    }
}
