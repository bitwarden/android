using System;
using Bit.App.Resources;
using Bit.Core;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class FormEntryTableViewCell : ExtendedUITableViewCell, ISelectable
    {
        public UILabel Label { get; set; }
        public UITextField TextField { get; set; }
        public UITextView TextView { get; set; }
        public UIButton Button { get; set; }
        public UIButton SecondButton { get; set; }
        public event EventHandler ValueChanged;

        public FormEntryTableViewCell(
            string labelName = null,
            bool useTextView = false,
            nfloat? height = null,
            ButtonsConfig buttonsConfig = ButtonsConfig.None,
            bool useLabelAsPlaceholder = false,
            float leadingConstant = 15f)
            : base(UITableViewCellStyle.Default, nameof(FormEntryTableViewCell))
        {
            var descriptor = UIFontDescriptor.PreferredBody;
            var pointSize = descriptor.PointSize;

            if (labelName != null && !useLabelAsPlaceholder)
            {
                Label = new UILabel
                {
                    Text = labelName,
                    TranslatesAutoresizingMaskIntoConstraints = false,
                    Font = UIFont.FromDescriptor(descriptor, 0.8f * pointSize),
                    TextColor = ThemeHelpers.MutedColor
                };

                ContentView.Add(Label);
            }

            if (useTextView)
            {
                TextView = new UITextView
                {
                    TranslatesAutoresizingMaskIntoConstraints = false,
                    Font = UIFont.FromDescriptor(descriptor, pointSize),
                    TextColor = ThemeHelpers.TextColor,
                    TintColor = ThemeHelpers.TextColor,
                    BackgroundColor = ThemeHelpers.BackgroundColor
                };

                if (!ThemeHelpers.LightTheme)
                {
                    TextView.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }

                ContentView.Add(TextView);
                ContentView.AddConstraints(new NSLayoutConstraint[] {
                    NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, leadingConstant),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, TextView, NSLayoutAttribute.Trailing, 1f, 15f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, TextView, NSLayoutAttribute.Bottom, 1f, 10f)
                });

                if (labelName != null && !useLabelAsPlaceholder)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Top, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Bottom, 1f, 10f));
                }
                else
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f));
                }

                if (height.HasValue)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextView, NSLayoutAttribute.Height, NSLayoutRelation.Equal, null, NSLayoutAttribute.NoAttribute, 1f, height.Value));
                }

                TextView.Changed += (object sender, EventArgs e) =>
                {
                    ValueChanged?.Invoke(sender, e);
                };
            }
            else
            {
                TextField = new UITextField
                {
                    TranslatesAutoresizingMaskIntoConstraints = false,
                    BorderStyle = UITextBorderStyle.None,
                    Font = UIFont.FromDescriptor(descriptor, pointSize),
                    ClearButtonMode = UITextFieldViewMode.WhileEditing,
                    TextColor = ThemeHelpers.TextColor,
                    TintColor = ThemeHelpers.TextColor,
                    BackgroundColor = ThemeHelpers.BackgroundColor
                };

                if (!ThemeHelpers.LightTheme)
                {
                    TextField.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }

                if (useLabelAsPlaceholder)
                {
                    TextField.Placeholder = labelName;
                }

                ContentView.Add(TextField);

                ContentView.AddConstraints(new NSLayoutConstraint[] {
                    NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, leadingConstant),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Trailing, 1f, GetTextFieldToContainerTrailingConstant(buttonsConfig)),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Bottom, 1f, 10f)
                });

                if (labelName != null && !useLabelAsPlaceholder)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Top, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Bottom, 1f, 10f));
                }
                else
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f));
                }

                if (height.HasValue)
                {
                    ContentView.AddConstraint(
                        NSLayoutConstraint.Create(TextField, NSLayoutAttribute.Height, NSLayoutRelation.Equal, null, NSLayoutAttribute.NoAttribute, 1f, height.Value));
                }

                TextField.AddTarget((object sender, EventArgs e) =>
                {
                    ValueChanged?.Invoke(sender, e);
                }, UIControlEvent.EditingChanged);
            }

            if (labelName != null && !useLabelAsPlaceholder)
            {
                ContentView.AddConstraints(new NSLayoutConstraint[] {
                    NSLayoutConstraint.Create(Label, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Leading, 1f, leadingConstant),
                    NSLayoutConstraint.Create(Label, NSLayoutAttribute.Top, NSLayoutRelation.Equal, ContentView, NSLayoutAttribute.Top, 1f, 10f),
                    NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, Label, NSLayoutAttribute.Trailing, 1f, 15f)
                });
            }

            if(buttonsConfig != ButtonsConfig.None)
            {
                AddButtons(buttonsConfig);
            }
        }

        public void Select()
        {
            if (TextView != null)
            {
                TextView.BecomeFirstResponder();
            }
            else if (TextField != null)
            {
                TextField.BecomeFirstResponder();
            }
        }

        public void ConfigureToggleSecureTextCell(bool useSecondaryButton = false)
        {
            var button = useSecondaryButton ? SecondButton : Button;
            button.TitleLabel.Font = UIFont.FromName("bwi-font", 28f);
            button.SetTitle(BitwardenIcons.Eye, UIControlState.Normal);
            button.AccessibilityLabel = AppResources.ToggleVisibility;
            button.AccessibilityHint = AppResources.PasswordIsNotVisibleTapToShow;
            button.TouchUpInside += (sender, e) =>
            {
                TextField.SecureTextEntry = !TextField.SecureTextEntry;
                button.SetTitle(TextField.SecureTextEntry ? BitwardenIcons.Eye : BitwardenIcons.EyeSlash, UIControlState.Normal);
                button.AccessibilityHint = TextField.SecureTextEntry ? AppResources.PasswordIsNotVisibleTapToShow : AppResources.PasswordIsVisibleTapToHide;
            };
        }

        private void AddButtons(ButtonsConfig buttonsConfig)
        {
            Button = new UIButton(UIButtonType.System);
            Button.TranslatesAutoresizingMaskIntoConstraints = false;
            Button.SetTitleColor(ThemeHelpers.PrimaryColor, UIControlState.Normal);

            ContentView.Add(Button);

            ContentView.BottomAnchor.ConstraintEqualTo(Button.BottomAnchor, 10f).Active = true;

            switch (buttonsConfig)
            {
                case ButtonsConfig.One:
                    ContentView.AddConstraints(new NSLayoutConstraint[] {
                        NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, Button, NSLayoutAttribute.Trailing, 1f, 10f),
                        NSLayoutConstraint.Create(Button, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Trailing, 1f, 10f)
                    });
                    break;
                case ButtonsConfig.Two:
                    SecondButton = new UIButton(UIButtonType.System);
                    SecondButton.TranslatesAutoresizingMaskIntoConstraints = false;
                    SecondButton.SetTitleColor(ThemeHelpers.PrimaryColor, UIControlState.Normal);

                    ContentView.Add(SecondButton);

                    ContentView.AddConstraints(new NSLayoutConstraint[] {
                        NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, SecondButton, NSLayoutAttribute.Bottom, 1f, 10f),
                        NSLayoutConstraint.Create(SecondButton, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, TextField, NSLayoutAttribute.Trailing, 1f, 9f),
                        NSLayoutConstraint.Create(Button, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, SecondButton, NSLayoutAttribute.Trailing, 1f, 10f),
                        NSLayoutConstraint.Create(ContentView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, Button, NSLayoutAttribute.Trailing, 1f, 10f)
                    });
                    break;
            }
        }

        private float GetTextFieldToContainerTrailingConstant(ButtonsConfig buttonsConfig)
        {
            switch (buttonsConfig)
            {
                case ButtonsConfig.None:
                    return 15f;
                case ButtonsConfig.One:
                    return 55f;
                case ButtonsConfig.Two:
                    return 95f;
                default:
                    throw new ArgumentOutOfRangeException();
            }
        }

        public enum ButtonsConfig : byte
        {
            None = 0,
            One = 1,
            Two = 2
        }
    }
}
