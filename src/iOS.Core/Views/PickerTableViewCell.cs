using System;
using System.Collections.Generic;
using System.Drawing;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class PickerTableViewCell : UITableViewCell
    {
        public PickerTableViewCell(string labelName)
            : base(UITableViewCellStyle.Default, nameof(PickerTableViewCell))
        {
            TextLabel.Text = labelName;

            var entry = new UITextField { BorderStyle = UITextBorderStyle.RoundedRect };
            entry.Started += Entry_Started;
            entry.Ended += Entry_Ended;

            var width = (float)UIScreen.MainScreen.Bounds.Width;
            var toolbar = new UIToolbar(new RectangleF(0, 0, width, 44))
            {
                BarStyle = UIBarStyle.Default,
                Translucent = true
            };
            var spacer = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
            var doneButton = new UIBarButtonItem(UIBarButtonSystemItem.Done, (o, a) =>
            {
                var s = (PickerSource)Picker.Model;
                if(s.SelectedIndex == -1 && Items != null && Items.Count > 0)
                {

                }
                entry.ResignFirstResponder();
            });

            toolbar.SetItems(new[] { spacer, doneButton }, false);

            entry.InputView = Picker;
            entry.InputAccessoryView = toolbar;
        }

        public UIPickerView Picker { get; set; } = new UIPickerView();
        public int MyProperty { get; set; }
        public List<string> Items { get; set; } = new List<string>();
        public int SelectedIndex { get; set; }

        private void Entry_Ended(object sender, EventArgs e)
        {
            //throw new NotImplementedException();
        }

        private void Entry_Started(object sender, EventArgs e)
        {
            //throw new NotImplementedException();
        }

        private class PickerSource : UIPickerViewModel
        {
            readonly PickerTableViewCell _cell;

            public PickerSource(PickerTableViewCell cell)
            {
                _cell = cell;
            }

            public int SelectedIndex { get; internal set; }

            public string SelectedItem { get; internal set; }

            public override nint GetComponentCount(UIPickerView picker)
            {
                return 1;
            }

            public override nint GetRowsInComponent(UIPickerView pickerView, nint component)
            {
                return _cell.Items != null ? _cell.Items.Count : 0;
            }

            public override string GetTitle(UIPickerView picker, nint row, nint component)
            {
                return _cell.Items[(int)row];
            }

            public override void Selected(UIPickerView picker, nint row, nint component)
            {
                if(_cell.Items.Count == 0)
                {
                    SelectedItem = null;
                    SelectedIndex = -1;
                }
                else
                {
                    SelectedItem = _cell.Items[(int)row];
                    SelectedIndex = (int)row;
                }

                //_renderer.UpdatePickerFromModel(this);
            }
        }
    }
}
