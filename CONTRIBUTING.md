Code contributions are welcome! Please commit any pull requests against the `master` branch.

# Internationalization (i18n)

If you are interested in helping translate the bitwarden mobile app into another language, please follow these steps
when creating your pull request:

1. Create a new resource file under `/src/App/Resources` by copy/pasting the master English file, `AppResources.resx`.
2. Rename your `AppResources.resx` copy to include the proper .NET culture code for your language (typically the two
   letter code, ex. `sv`). You can find a list of culture codes here: <http://timtrott.co.uk/culture-codes/>. For
   example, if I want to create a new translation for Swedish, I will rename my `AppResources.resx` copy to
   `AppResources.sv.resx`.
3. Open the `AppResources.XX.resx` file for your newly created language and start translating the `<value>` tags for
   each `<data>` element. The `<data>` and `<comment>` properties should not be translated and remain in English.
4. Repeat the same process for the store `COPY.md` and `CAPTIONS.md` files in `/store/apple` and `/store/google` by
   creating a new folder for your language in each. Do not copy over the `assets` and `screenshots` folders to your new
   language. We will update these based on your translations provided in `CAPTIONS.md`. Finally, do not translate the
   titles in the markdown files (ex. `# Name` and `# Screenshot 1`). These are only for reference.
5. If you have a Xamarin development environment setup, test your translations to make sure they look correct in the
   app on iOS and Android. Sometimes the UI can break due to translations taking up more space than the original UI was
   built for. If possible, use a shorter or abbreviated version of the word/sentence to accomedate the available space.
   If you are inable to accomedate the avaialable space for a particular translation, just let us know in your pull
   request comments. If you are unable to test your translations, just let us know in your pull request comments so
   that we can check it for you.
6. Be sure to watch for [future changes](https://github.com/bitwarden/mobile/commits/master/src/App/Resources/AppResources.resx)
   to the `/src/App/Resources/AppResources.resx` file so that your translation will stay up to date.

You can find an example of a proper translation pull request here: <https://github.com/bitwarden/mobile/pull/22/files>

You can read more about localizing a Xamarin.Forms app here:
<https://developer.xamarin.com/guides/xamarin-forms/advanced/localization/>

TIP: If you have Visual Studio installed, it provides a nice tabular UI for editing `resx` XML files.