var BitwardenExtension = function () { };

BitwardenExtension.prototype = {
    run: function (arguments) {
        console.log('Run');
        console.log(arguments);

        var args = {
            url: document.URL
        };
        arguments.completionFunction(args);
    },
    finalize: function (arguments) {
        console.log('Finalize');
        console.log(arguments);

        if (arguments.username || arguments.password) {
            this.fillDocument(arguments.username, arguments.password, arguments.autoSubmit);
        }
    },

    getSubmitButton: function (form) {
        var button;
        for (var i = 0; i < form.elements.length; i++) {
            if (form.elements[i].type == 'submit') {
                button = form.elements[i];
                break;
            }
        }

        if (!button) {
            console.log('cannot locate submit button');
            return null;
        }

        return button;
    },

    // Thanks Mozilla!
    // ref: http://mxr.mozilla.org/firefox/source/toolkit/components/passwordmgr/src/nsLoginManager.js?raw=1

    /* ***** BEGIN LICENSE BLOCK *****
     * Version: MPL 1.1/GPL 2.0/LGPL 2.1
     *
     * The contents of this file are subject to the Mozilla Public License Version
     * 1.1 (the "License"); you may not use this file except in compliance with
     * the License. You may obtain a copy of the License at
     * http://www.mozilla.org/MPL/
     *
     * Software distributed under the License is distributed on an "AS IS" basis,
     * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
     * for the specific language governing rights and limitations under the
     * License.
     *
     * The Original Code is mozilla.org code.
     *
     * The Initial Developer of the Original Code is Mozilla Corporation.
     * Portions created by the Initial Developer are Copyright (C) 2007
     * the Initial Developer. All Rights Reserved.
     *
     * Contributor(s):
     *  Justin Dolske <dolske@mozilla.com> (original author)
     *
     * Alternatively, the contents of this file may be used under the terms of
     * either the GNU General Public License Version 2 or later (the "GPL"), or
     * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
     * in which case the provisions of the GPL or the LGPL are applicable instead
     * of those above. If you wish to allow use of your version of this file only
     * under the terms of either the GPL or the LGPL, and not to allow others to
     * use your version of this file under the terms of the MPL, indicate your
     * decision by deleting the provisions above and replace them with the notice
     * and other provisions required by the GPL or the LGPL. If you do not delete
     * the provisions above, a recipient may use your version of this file under
     * the terms of any one of the MPL, the GPL or the LGPL.
     *
     * ***** END LICENSE BLOCK ***** */

    /*
     * Returns an array of password field elements for the specified form.
     * If no pw fields are found, or if more than 3 are found, then null
     * is returned.
     *
     * skipEmptyFields can be set to ignore password fields with no value.
     */
    getPasswordFields: function (form, skipEmptyFields) {
        // Locate the password fields in the form.
        var pwFields = [];
        for (var i = 0; i < form.elements.length; i++) {
            if (form.elements[i].type != 'password') {
                continue;
            }

            if (skipEmptyFields && !form.elements[i].value) {
                continue;
            }

            pwFields[pwFields.length] = {
                index: i,
                element: form.elements[i]
            };
        }

        // If too few or too many fields, bail out.
        if (pwFields.length == 0) {
            console.log('form ignored -- no password fields.');
            return null;
        }
        else if (pwFields.length > 3) {
            console.log('form ignored -- too many password fields. got ' + pwFields.length + '.');
            return null;
        }

        return pwFields;
    },
    /*
     * Returns the username and password fields found in the form.
     * Can handle complex forms by trying to figure out what the
     * relevant fields are.
     *
     * Returns: [usernameField, newPasswordField, oldPasswordField]
     *
     * usernameField may be null.
     * newPasswordField will always be non-null.
     * oldPasswordField may be null. If null, newPasswordField is just
     * "theLoginField". If not null, the form is apparently a
     * change-password field, with oldPasswordField containing the password
     * that is being changed.
     */
    getFormFields: function (form, isSubmission) {
        var usernameField = null,
            submitButton = null;

        // Locate the password field(s) in the form. Up to 3 supported.
        // If there's no password field, there's nothing for us to do.
        var pwFields = this.getPasswordFields(form, isSubmission);
        if (!pwFields) {
            return [null, null, null, null];
        }

        submitButton = this.getSubmitButton(form);

        // Locate the username field in the form by searching backwards
        // from the first passwordfield, assume the first text field is the
        // username. We might not find a username field if the user is
        // already logged in to the site. 
        for (var i = pwFields[0].index - 1; i >= 0; i--) {
            if (form.elements[i].type == 'text'
                || form.elements[i].type == 'email'
                || form.elements[i].type == 'tel') {
                usernameField = form.elements[i];
                break;
            }
        }

        if (!usernameField) {
            console.log('form -- no username field found');
        }

        // If we're not submitting a form (it's a page load), there are no
        // password field values for us to use for identifying fields. So,
        // just assume the first password field is the one to be filled in.
        if (!isSubmission || pwFields.length == 1) {
            return [usernameField, pwFields[0].element, null, submitButton];
        }

        // Try to figure out WTF is in the form based on the password values.
        var oldPasswordField, newPasswordField;
        var pw1 = pwFields[0].element.value;
        var pw2 = pwFields[1].element.value;
        var pw3 = (pwFields[2] ? pwFields[2].element.value : null);

        if (pwFields.length == 3) {
            // Look for two identical passwords, that's the new password

            if (pw1 == pw2 && pw2 == pw3) {
                // All 3 passwords the same? Weird! Treat as if 1 pw field.
                newPasswordField = pwFields[0].element;
                oldPasswordField = null;
            }
            else if (pw1 == pw2) {
                newPasswordField = pwFields[0].element;
                oldPasswordField = pwFields[2].element;
            }
            else if (pw2 == pw3) {
                oldPasswordField = pwFields[0].element;
                newPasswordField = pwFields[2].element;
            }
            else if (pw1 == pw3) {
                // A bit odd, but could make sense with the right page layout.
                newPasswordField = pwFields[0].element;
                oldPasswordField = pwFields[1].element;
            }
            else {
                // We can't tell which of the 3 passwords should be saved.
                console.log('form ignored -- all 3 pw fields differ');
                return [null, null, null, null];
            }
        }
        else { // pwFields.length == 2
            if (pw1 == pw2) {
                // Treat as if 1 pw field
                newPasswordField = pwFields[0].element;
                oldPasswordField = null;
            }
            else {
                // Just assume that the 2nd password is the new password
                oldPasswordField = pwFields[0].element;
                newPasswordField = pwFields[1].element;
            }
        }

        return [usernameField, newPasswordField, oldPasswordField, submitButton];
    },
    fillDocument: function (username, password, autoSubmit) {
        if (!password) {
            return;
        }

        if (!document.forms || document.forms.length === 0) {
            return;
        }

        for (var i = 0; i < document.forms.length; i++) {
            var fields = this.getFormFields(document.forms[i], false);
            var usernameField = fields[0],
                passwordField = fields[1],
                submitButton = fields[3];

            if (!usernameField && !passwordField) {
                console.log('cannot locate fields in form #' + i);
                continue;
            }

            var maxUsernameLength = Number.MAX_VALUE,
                maxPasswordLength = Number.MAX_VALUE;

            var filledUsername = false,
                filledPassword = false;

            if (username && usernameField) {
                if (usernameField.maxLength >= 0) {
                    maxUsernameLength = usernameField.maxLength;
                }
                if (username.length <= maxUsernameLength) {
                    usernameField.value = username;
                    filledUsername = true;
                }
            }

            if (passwordField) {
                if (passwordField.maxLength >= 0) {
                    maxPasswordLength = passwordField.maxLength;
                }
                if (password.length <= maxPasswordLength) {
                    passwordField.value = password;
                    filledPassword = true;
                }
            }

            if (autoSubmit && filledPassword && filledPassword) {
                setTimeout(function () {
                    if (submitButton) {
                        submitButton.click();
                    }
                    else {
                        document.forms[i].submit();
                    }
                }, 500);

                break;
            }
        }
    }
};

var ExtensionPreprocessingJS = new BitwardenExtension;
