// WARNING
//
// This file has been generated automatically by Visual Studio from the outlets and
// actions declared in your storyboard file.
// Manual changes to this file will not be maintained.
//
using Foundation;
using System;
using System.CodeDom.Compiler;
using UIKit;

namespace Bit.iOS.Autofill
{
    [Register ("CredentialProviderViewController")]
    partial class CredentialProviderViewController
    {
        [Outlet]
        [GeneratedCode ("iOS Designer", "1.0")]
        UIKit.UIImageView Logo { get; set; }

        void ReleaseDesignerOutlets ()
        {
            if (Logo != null) {
                Logo.Dispose ();
                Logo = null;
            }
        }
    }
}