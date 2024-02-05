using Bit.Core.Utilities;

#nullable enable

namespace Bit.App.Controls
{
    public struct AvatarInfo
    {
        private const string DEFAULT_BACKGROUND_COLOR = "#33ffffff";

        public AvatarInfo(string? userId = null, string? name = null, string? email = null, string? color = null, int size = 50)
        {
            Size = size;
            var text = string.IsNullOrWhiteSpace(name) ? email : name;

            string? upperCaseText = null;

            if (string.IsNullOrEmpty(text))
            {
                CharsToDraw = "..";
            }
            else if (text.Length > 1)
            {
                upperCaseText = text.ToUpper();
                CharsToDraw = GetFirstLetters(upperCaseText, 2);
            }
            else
            {
                CharsToDraw = upperCaseText = text.ToUpper();
            }

            BackgroundColor = color ?? CoreHelpers.StringToColor(userId ?? upperCaseText, DEFAULT_BACKGROUND_COLOR);
            TextColor = CoreHelpers.TextColorFromBgColor(BackgroundColor);
        }

        public string CharsToDraw { get; }
        public string BackgroundColor { get; }
        public string TextColor { get; }
        public int Size { get; }

        private static string GetFirstLetters(string data, int charCount)
        {
            var sanitizedData = data.Trim();
            var parts = sanitizedData.Split(new char[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);

            if (parts.Length > 1 && charCount <= 2)
            {
                var text = string.Empty;
                for (var i = 0; i < charCount; i++)
                {
                    text += parts[i][0];
                }
                return text;
            }
            if (sanitizedData.Length > 2)
            {
                return sanitizedData.Substring(0, 2);
            }
            return sanitizedData;
        }
    }
}

