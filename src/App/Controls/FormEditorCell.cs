using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormEditorCell : ExtendedViewCell
    {
        public FormEditorCell(Keyboard entryKeyboard = null, double? height = null)
        {
            Editor = new ExtendedEditor
            {
                Keyboard = entryKeyboard,
                HasBorder = false
            };

            if(height.HasValue)
            {
                Editor.HeightRequest = height.Value;
            }

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15, 10)
            };

            stackLayout.Children.Add(Editor);

            Tapped += FormEditorCell_Tapped;

            if(Device.OS == TargetPlatform.Android)
            {
                Editor.FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label));
                Editor.Margin = new Thickness(-4, -2, -4, -10);
            }

            View = stackLayout;
        }

        public ExtendedEditor Editor { get; private set; }

        private void FormEditorCell_Tapped(object sender, EventArgs e)
        {
            Editor.Focus();
        }
    }
}
