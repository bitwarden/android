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
            Label.SetBinding<VaultListPageModel.Login>(Label.TextProperty, s => s.Name);
            Detail.SetBinding<VaultListPageModel.Login>(Label.TextProperty, s => s.Username);

            Button.Image = "more";
            Button.Command = new Command(() => moreClickedAction?.Invoke(LoginParameter));
            Button.BackgroundColor = Color.Transparent;

            BackgroundColor = Color.White;
        }

        public VaultListPageModel.Login LoginParameter
        {
            get { return GetValue(LoginParameterProperty) as VaultListPageModel.Login; }
            set { SetValue(LoginParameterProperty, value); }
        }
    }
}
