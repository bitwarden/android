// WARNING
//
// This file has been generated automatically by Xamarin Studio from the outlets and
// actions declared in your storyboard file.
// Manual changes to this file will not be maintained.
//
using Foundation;
using System;
using System.CodeDom.Compiler;
using UIKit;

namespace Bit.iOS.Extension
{
	[Register ("ActionViewController")]
	partial class ActionViewController
	{
		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIImageView logo { get; set; }

		[Action ("DoneClicked:")]
		partial void DoneClicked (Foundation.NSObject sender);

		void ReleaseDesignerOutlets ()
		{
			if (logo != null) {
				logo.Dispose ();
				logo = null;
			}
		}
	}
}
