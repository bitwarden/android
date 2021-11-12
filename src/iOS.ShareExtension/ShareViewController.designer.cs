// WARNING
//
// This file has been generated automatically by Visual Studio to store outlets and
// actions made in the UI designer. If it is removed, they will be lost.
// Manual changes to this file may not be handled correctly.
//
using Foundation;
using System.CodeDom.Compiler;

namespace Bit.iOS.ShareExtension
{
	[Register ("ShareViewController")]
	partial class ShareViewController
	{
		[Outlet]
		UIKit.UIBarButtonItem _cancelButton { get; set; }

		[Outlet]
		UIKit.UIView _contentView { get; set; }

		[Outlet]
		UIKit.UILabel _deletionDateDescriptionLabel { get; set; }

		[Outlet]
		UIKit.UILabel _deletionDateLabel { get; set; }

		[Outlet]
		UIKit.UILabel _deletionDateValueLabel { get; set; }

		[Outlet]
		UIKit.UILabel _disableSendFromAccessLabel { get; set; }

		[Outlet]
		UIKit.UISwitch _disableSendFromAccessSwitch { get; set; }

		[Outlet]
		UIKit.UILabel _expirationDateDescriptionLabel { get; set; }

		[Outlet]
		UIKit.UILabel _expirationDateLabel { get; set; }

		[Outlet]
		UIKit.UILabel _expirationDateValueLabel { get; set; }

		[Outlet]
		UIKit.UILabel _fileLabel { get; set; }

		[Outlet]
		UIKit.UILabel _hideEmailFromRecipientsLabel { get; set; }

		[Outlet]
		UIKit.UISwitch _hideEmailFromRecipientsSwitch { get; set; }

		[Outlet]
		UIKit.UIImageView _imageView { get; set; }

		[Outlet]
		UIKit.UILabel _maximumAccessCountDescriptionLabel { get; set; }

		[Outlet]
		UIKit.UITextField _maximumAccessCountField { get; set; }

		[Outlet]
		UIKit.UILabel _maximumAccessCountLabel { get; set; }

		[Outlet]
		UIKit.UIStepper _maximumAccessCountStepper { get; set; }

		[Outlet]
		UIKit.UILabel _nameDescriptionLabel { get; set; }

		[Outlet]
		UIKit.UITextField _nameField { get; set; }

		[Outlet]
		UIKit.UILabel _nameLabel { get; set; }

		[Outlet]
		UIKit.UINavigationItem _navItem { get; set; }

		[Outlet]
		UIKit.UILabel _newPasswordDescriptionLabel { get; set; }

		[Outlet]
		UIKit.UIImageView _newPasswordEye { get; set; }

		[Outlet]
		UIKit.UITextField _newPasswordField { get; set; }

		[Outlet]
		UIKit.UILabel _newPasswordLabel { get; set; }

		[Outlet]
		UIKit.UILabel _notesDescriptionLabel { get; set; }

		[Outlet]
		UIKit.UILabel _notesLabel { get; set; }

		[Outlet]
		UIKit.UITextView _notesTextView { get; set; }

		[Outlet]
		UIKit.UILabel _optionsLabel { get; set; }

		[Outlet]
		UIKit.UIScrollView _scrollView { get; set; }

		[Outlet]
		UIKit.UIBarButtonItem _sendButton { get; set; }

		[Outlet]
		UIKit.UILabel _shareUponSaveLabel { get; set; }

		[Outlet]
		UIKit.UISwitch _shareUponSaveSwitch { get; set; }

		[Action ("Cancel_Activated:")]
		partial void Cancel_Activated (UIKit.UIBarButtonItem sender);

		[Action ("Send_Activated:")]
		partial void Send_Activated (UIKit.UIBarButtonItem sender);
		
		void ReleaseDesignerOutlets ()
		{
			if (_cancelButton != null) {
				_cancelButton.Dispose ();
				_cancelButton = null;
			}

			if (_imageView != null) {
				_imageView.Dispose ();
				_imageView = null;
			}

			if (_nameField != null) {
				_nameField.Dispose ();
				_nameField = null;
			}

			if (_nameLabel != null) {
				_nameLabel.Dispose ();
				_nameLabel = null;
			}

			if (_navItem != null) {
				_navItem.Dispose ();
				_navItem = null;
			}

			if (_sendButton != null) {
				_sendButton.Dispose ();
				_sendButton = null;
			}

			if (_fileLabel != null) {
				_fileLabel.Dispose ();
				_fileLabel = null;
			}

			if (_nameDescriptionLabel != null) {
				_nameDescriptionLabel.Dispose ();
				_nameDescriptionLabel = null;
			}

			if (_shareUponSaveLabel != null) {
				_shareUponSaveLabel.Dispose ();
				_shareUponSaveLabel = null;
			}

			if (_shareUponSaveSwitch != null) {
				_shareUponSaveSwitch.Dispose ();
				_shareUponSaveSwitch = null;
			}

			if (_optionsLabel != null) {
				_optionsLabel.Dispose ();
				_optionsLabel = null;
			}

			if (_deletionDateLabel != null) {
				_deletionDateLabel.Dispose ();
				_deletionDateLabel = null;
			}

			if (_deletionDateValueLabel != null) {
				_deletionDateValueLabel.Dispose ();
				_deletionDateValueLabel = null;
			}

			if (_deletionDateDescriptionLabel != null) {
				_deletionDateDescriptionLabel.Dispose ();
				_deletionDateDescriptionLabel = null;
			}

			if (_expirationDateLabel != null) {
				_expirationDateLabel.Dispose ();
				_expirationDateLabel = null;
			}

			if (_expirationDateValueLabel != null) {
				_expirationDateValueLabel.Dispose ();
				_expirationDateValueLabel = null;
			}

			if (_expirationDateDescriptionLabel != null) {
				_expirationDateDescriptionLabel.Dispose ();
				_expirationDateDescriptionLabel = null;
			}

			if (_maximumAccessCountLabel != null) {
				_maximumAccessCountLabel.Dispose ();
				_maximumAccessCountLabel = null;
			}

			if (_maximumAccessCountField != null) {
				_maximumAccessCountField.Dispose ();
				_maximumAccessCountField = null;
			}

			if (_maximumAccessCountStepper != null) {
				_maximumAccessCountStepper.Dispose ();
				_maximumAccessCountStepper = null;
			}

			if (_maximumAccessCountDescriptionLabel != null) {
				_maximumAccessCountDescriptionLabel.Dispose ();
				_maximumAccessCountDescriptionLabel = null;
			}

			if (_newPasswordLabel != null) {
				_newPasswordLabel.Dispose ();
				_newPasswordLabel = null;
			}

			if (_newPasswordField != null) {
				_newPasswordField.Dispose ();
				_newPasswordField = null;
			}

			if (_newPasswordEye != null) {
				_newPasswordEye.Dispose ();
				_newPasswordEye = null;
			}

			if (_newPasswordDescriptionLabel != null) {
				_newPasswordDescriptionLabel.Dispose ();
				_newPasswordDescriptionLabel = null;
			}

			if (_notesLabel != null) {
				_notesLabel.Dispose ();
				_notesLabel = null;
			}

			if (_notesTextView != null) {
				_notesTextView.Dispose ();
				_notesTextView = null;
			}

			if (_notesDescriptionLabel != null) {
				_notesDescriptionLabel.Dispose ();
				_notesDescriptionLabel = null;
			}

			if (_hideEmailFromRecipientsLabel != null) {
				_hideEmailFromRecipientsLabel.Dispose ();
				_hideEmailFromRecipientsLabel = null;
			}

			if (_hideEmailFromRecipientsSwitch != null) {
				_hideEmailFromRecipientsSwitch.Dispose ();
				_hideEmailFromRecipientsSwitch = null;
			}

			if (_disableSendFromAccessLabel != null) {
				_disableSendFromAccessLabel.Dispose ();
				_disableSendFromAccessLabel = null;
			}

			if (_disableSendFromAccessSwitch != null) {
				_disableSendFromAccessSwitch.Dispose ();
				_disableSendFromAccessSwitch = null;
			}

			if (_scrollView != null) {
				_scrollView.Dispose ();
				_scrollView = null;
			}

			if (_contentView != null) {
				_contentView.Dispose ();
				_contentView = null;
			}
		}
	}
}
