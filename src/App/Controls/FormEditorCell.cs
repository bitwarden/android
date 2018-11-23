using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormEditorCell : ExtendedViewCell, IDisposable
    {
        public FormEditorCell(Keyboard entryKeyboard = null, double? height = null)
        {
            Editor = new ExtendedEditor
            {
                Keyboard = entryKeyboard,
                HasBorder = false,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Editor))
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

            Editor.AdjustMarginsForDevice();
            stackLayout.AdjustPaddingForDevice();

            View = stackLayout;
        }

        public ExtendedEditor Editor { get; private set; }

        private void FormEditorCell_Tapped(object sender, EventArgs e)
        {
            Editor.Focus();
        }

        public void InitEvents()
        {
            Tapped += FormEditorCell_Tapped;
        }

        public void Dispose()
        {
            Tapped -= FormEditorCell_Tapped;
        }
    }
}
