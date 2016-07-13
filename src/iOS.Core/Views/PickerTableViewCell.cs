using CoreGraphics;
using System;
using System.Collections.Generic;
using System.Drawing;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class PickerTableViewCell : UITableViewCell, ISelectable
    {
        private List<string> _items = new List<string>();
        private int _selectedIndex = 0;

        public PickerTableViewCell(
            string labelName,
            nfloat? height = null)
            : base(UITableViewCellStyle.Default, nameof(PickerTableViewCell))
        {
            var descriptor = UIFontDescriptor.PreferredBody;
            var pointSize = descriptor.PointSize;

            Label = new UILabel
            {
                Text = labelName,
                TranslatesAutoresizingMaskIntoConstraints = false,
                Font = UIFont.FromDescriptor(descriptor, 0.8f * pointSize),
                TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f)
            };

            ContentView.Add(Label);

            TextField = new NoCaretField
            {
                BorderStyle = UITextBorderStyle.None,
                TranslatesAutoresizingMaskIntoConstraints = false,
                Font = UIFont.FromDescriptor(descriptor, pointSize)
            };

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
                    UpdatePickerSelectedIndex(0);
                }
                TextField.Text = s.SelectedItem;
                TextField.ResignFirstResponder();
            });

            toolbar.SetItems(new[] { spacer, doneButton }, false);

            TextField.InputView = Picker;
            TextField.InputAccessoryView = toolbar;

            ContentView.Add(TextField);

            ContentView.AddConstraints(new NSLayoutConstraint[] {
                NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, 15f),
                NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Trailing, 1f, 15f),
                NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Bottom, 1f, 10f),
                NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Top, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Bottom, 1f, 10f),
                NSLayoutConstraint.Create(Label, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, 15f),
                NSLayoutConstraint.Create(Label, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f),
                NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Trailing, 1f, 15f)
            });

            if(height.HasValue)
            {
                ContentView.AddConstraint(
                    NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Height, NSLayoutRelation.Equal, null, NSLayoutAttribute.NoAttribute, 1f, height.Value));
            }

            Picker.Model = new PickerSource(this);
        }

        public UITextField TextField { get; set; }
        public UILabel Label { get; set; }
        public UIPickerView Picker { get; set; } = new UIPickerView();

        public List<string> Items
        {
            get { return _items; }
            set
            {
                _items = value;
                UpdatePicker();
            }
        }

        public int SelectedIndex
        {
            get { return _selectedIndex; }
            set
            {
                _selectedIndex = value;
                UpdatePicker();
            }
        }

        public string SelectedItem => TextField.Text;

        private void UpdatePicker()
        {
            TextField.Text = SelectedIndex == -1 || Items == null ? "" : Items[SelectedIndex];
            Picker.ReloadAllComponents();
            if(Items == null || Items.Count == 0)
            {
                return;
            }

            UpdatePickerSelectedIndex(SelectedIndex);
        }

        private void UpdatePickerFromModel(PickerSource s)
        {
            TextField.Text = s.SelectedItem;
            _selectedIndex = s.SelectedIndex;
        }

        private void UpdatePickerSelectedIndex(int formsIndex)
        {
            var source = (PickerSource)Picker.Model;
            source.SelectedIndex = formsIndex;
            source.SelectedItem = formsIndex >= 0 ? Items[formsIndex] : null;
            Picker.Select(Math.Max(formsIndex, 0), 0, true);
        }

        public void Select()
        {
            TextField?.BecomeFirstResponder();
        }

        private class NoCaretField : UITextField
        {
            public NoCaretField() : base(default(CGRect))
            { }

            public override CGRect GetCaretRectForPosition(UITextPosition position)
            {
                return default(CGRect);
            }
        }

        private class PickerSource : UIPickerViewModel
        {
            private readonly PickerTableViewCell _cell;

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

                _cell.UpdatePickerFromModel(this);
            }
        }
    }
}
