using Bit.App.Models.Page;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class VaultListViewCell : LabeledDetailCell
    {
        public static readonly BindableProperty CipherParameterProperty = BindableProperty.Create(nameof(CipherParameter),
            typeof(VaultListPageModel.Cipher), typeof(VaultListViewCell), null);

        public VaultListViewCell(Action<VaultListPageModel.Cipher> moreClickedAction)
        {
            SetBinding(CipherParameterProperty, new Binding("."));
            Label.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Cipher.Name));
            Detail.SetBinding(Label.TextProperty, nameof(VaultListPageModel.Cipher.Subtitle));
            LabelIcon.SetBinding(VisualElement.IsVisibleProperty, nameof(VaultListPageModel.Cipher.Shared));
            LabelIcon2.SetBinding(VisualElement.IsVisibleProperty, nameof(VaultListPageModel.Cipher.HasAttachments));

            Button.Image = "more.png";
            Button.Command = new Command(() => moreClickedAction?.Invoke(CipherParameter));
            Button.BackgroundColor = Color.Transparent;

            LabelIcon.Source = "share.png";
            LabelIcon2.Source = "paperclip.png";

            BackgroundColor = Color.White;
        }

        public VaultListPageModel.Cipher CipherParameter
        {
            get { return GetValue(CipherParameterProperty) as VaultListPageModel.Cipher; }
            set { SetValue(CipherParameterProperty, value); }
        }

        protected override void OnBindingContextChanged()
        {
            Icon.Source = null;
            if(BindingContext is VaultListPageModel.Cipher item)
            {
                if(item.Type == Enums.CipherType.Login)
                {
                    Icon.LoadingPlaceholder = "login.png";
                }
                Icon.Source = item.Icon;
            }

            base.OnBindingContextChanged();
        }
    }
}
