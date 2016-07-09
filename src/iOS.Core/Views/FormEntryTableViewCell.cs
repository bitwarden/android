using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class FormEntryTableViewCell : UITableViewCell
    {
        public FormEntryTableViewCell(
            string labelName = null,
            bool useTextView = false,
            nfloat? height = null)
            : base(UITableViewCellStyle.Default, nameof(FormEntryTableViewCell))
        {
            var descriptor = UIFontDescriptor.PreferredBody;
            var pointSize = descriptor.PointSize;

            if(labelName != null)
            {
                Label = new UILabel
                {
                    Text = labelName,
                    TranslatesAutoresizingMaskIntoConstraints = false,
                    Font = UIFont.FromDescriptor(descriptor, 0.8f * pointSize),
                    TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f)
                };

                ContentView.Add(Label);
            }

            if(useTextView)
            {
                TextView = new UITextView
                {
                    TranslatesAutoresizingMaskIntoConstraints = false,
                    Font = UIFont.FromDescriptor(descriptor, pointSize)
                };

                ContentView.Add(TextView);
                ContentView.AddConstraints(new NSLayoutConstraint[] {
                    NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, 15f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, TextView, NSLayoutAttribute.Trailing, 1f, 15f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, TextView, NSLayoutAttribute.Bottom, 1f, 10f)
                });

                if(labelName != null)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Top, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Bottom, 1f, 10f));
                }
                else
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f));
                }

                if(height.HasValue)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Height, NSLayoutRelation.Equal, null, NSLayoutAttribute.NoAttribute, 1f, height.Value));
                }
            }
            else
            {
                TextField = new UITextField
                {
                    TranslatesAutoresizingMaskIntoConstraints = false,
                    BorderStyle = UITextBorderStyle.None,
                    Font = UIFont.FromDescriptor(descriptor, pointSize),
                    ClearButtonMode = UITextFieldViewMode.WhileEditing
                };

                ContentView.Add(TextField);
                ContentView.AddConstraints(new NSLayoutConstraint[] {
                    NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, 15f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Trailing, 1f, 15f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Bottom, 1f, 10f)
                });

                if(labelName != null)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Top, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Bottom, 1f, 10f));
                }
                else
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f));
                }

                if(height.HasValue)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Height, NSLayoutRelation.Equal, null, NSLayoutAttribute.NoAttribute, 1f, height.Value));
                }
            }

            if(labelName != null)
            {
                ContentView.AddConstraints(new NSLayoutConstraint[] {
                    NSLayoutConstraint.Create(Label, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, 15f),
                    NSLayoutConstraint.Create(Label, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Trailing, 1f, 15f)
                });
            }
        }

        public UILabel Label { get; set; }
        public UITextField TextField { get; set; }
        public UITextView TextView { get; set; }
    }
}
