using SkiaSharp;

namespace Bit.App.Controls
{
    public class AvatarImageSource : StreamImageSource
    {
        private readonly string _text;
        private readonly string _id;
        private readonly string _color;
        private readonly AvatarInfo _avatarInfo;

        public override bool Equals(object obj)
        {
            if (obj is null)
            {
                return false;
            }

            if (obj is AvatarImageSource avatar)
            {
                return avatar._id == _id && avatar._text == _text && avatar._color == _color;
            }

            return base.Equals(obj);
        }

        public override int GetHashCode() => _id?.GetHashCode() ?? _text?.GetHashCode() ?? -1;

        public AvatarImageSource(string userId = null, string name = null, string email = null, string color = null)
        {
            _id = userId;
            _text = name;
            if (string.IsNullOrWhiteSpace(_text))
            {
                _text = email;
            }
            _color = color;

            //Workaround: [MAUI-Migration] There is currently a bug in MAUI where the actual size of the image is used instead of the size it should occupy in the Toolbar.
            //This causes some issues with the position of the icon. As a workaround we make the icon smaller until this is fixed.
            //Github issues: https://github.com/dotnet/maui/issues/12359  and  https://github.com/dotnet/maui/pull/17120
            _avatarInfo = new AvatarInfo(userId, name, email, color, DeviceInfo.Platform == DevicePlatform.iOS ? 20 : 50);
        }

        public override Func<CancellationToken, Task<Stream>> Stream => GetStreamAsync;

        private Task<Stream> GetStreamAsync(CancellationToken userToken = new CancellationToken())
        {
            var result = Draw();
            return Task.FromResult(result);
        }

        private Stream Draw()
        {
            using (var img = SKAvatarImageHelper.Draw(_avatarInfo))
            {
                var data = img.Encode(SKEncodedImageFormat.Png, 100);
                return data?.AsStream(true);
            }
        }
    }
}
